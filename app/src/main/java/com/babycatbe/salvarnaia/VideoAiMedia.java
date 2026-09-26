package com.babycatbe.salvarnaia;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VideoAiMedia {

    private static final int MAX_IMAGE_EDGE = 640;
    private static final long MAX_AUDIO_DURATION_US = 180L * 1_000_000L;

    private VideoAiMedia() {
    }

    public static PreparedMedia prepare(Context context, Uri videoUri) throws Exception {
        File workDir = new File(
                context.getCacheDir(),
                "local_ai_" + System.nanoTime()
        );
        if (!workDir.mkdirs()) {
            throw new IllegalStateException("Não foi possível preparar os arquivos da IA");
        }

        List<File> images = new ArrayList<>();
        File audio = null;
        long durationMs = 0L;

        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, videoUri);
                String rawDuration = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                );
                if (rawDuration != null) {
                    try {
                        durationMs = Long.parseLong(rawDuration);
                    } catch (NumberFormatException ignored) {
                    }
                }

                long[] timesUs = sampleTimes(durationMs);
                int imageIndex = 1;
                for (long timeUs : timesUs) {
                    Bitmap frame = retriever.getFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST
                    );
                    if (frame == null) continue;

                    Bitmap prepared = resize(frame);
                    File image = new File(workDir, "frame_" + imageIndex + ".jpg");
                    try (FileOutputStream out = new FileOutputStream(image)) {
                        prepared.compress(Bitmap.CompressFormat.JPEG, 84, out);
                    }

                    if (prepared != frame) prepared.recycle();
                    frame.recycle();

                    if (image.isFile() && image.length() > 0) {
                        images.add(image);
                        imageIndex++;
                    }
                }
            } finally {
                retriever.release();
            }

            File audioCandidate = new File(workDir, "audio.wav");
            boolean hasAudio = AudioWavExtractor.extract(
                    context,
                    videoUri,
                    audioCandidate,
                    MAX_AUDIO_DURATION_US
            );
            if (hasAudio) {
                audio = audioCandidate;
            } else {
                audioCandidate.delete();
            }

            if (images.isEmpty() && audio == null) {
                throw new IllegalStateException(
                        "Não foi possível extrair imagem nem áudio do vídeo"
                );
            }

            return new PreparedMedia(workDir, images, audio, durationMs);
        } catch (Exception e) {
            deleteRecursively(workDir);
            throw e;
        }
    }

    private static long[] sampleTimes(long durationMs) {
        if (durationMs <= 0) return new long[]{0L};

        double[] fractions = {0.08, 0.30, 0.55, 0.80, 0.96};
        Set<Long> unique = new HashSet<>();
        for (double fraction : fractions) {
            long millis = Math.max(
                    0L,
                    Math.min(durationMs - 1L, Math.round(durationMs * fraction))
            );
            unique.add(millis * 1000L);
        }

        long[] result = new long[unique.size()];
        int index = 0;
        for (Long time : unique) result[index++] = time;
        java.util.Arrays.sort(result);
        return result;
    }

    private static Bitmap resize(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int max = Math.max(width, height);
        if (max <= MAX_IMAGE_EDGE) return source;

        float scale = MAX_IMAGE_EDGE / (float) max;
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(
                source,
                targetWidth,
                targetHeight,
                true
        );
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }

    public static final class PreparedMedia implements AutoCloseable {
        public final List<File> images;
        public final File audio;
        public final long durationMs;

        private final File workDir;

        PreparedMedia(
                File workDir,
                List<File> images,
                File audio,
                long durationMs
        ) {
            this.workDir = workDir;
            this.images = images;
            this.audio = audio;
            this.durationMs = durationMs;
        }

        @Override
        public void close() {
            deleteRecursively(workDir);
        }
    }
}
