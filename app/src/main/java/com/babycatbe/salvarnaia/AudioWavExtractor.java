package com.babycatbe.salvarnaia;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class AudioWavExtractor {

    private static final int TARGET_SAMPLE_RATE = 16_000;

    private AudioWavExtractor() {
    }

    public static boolean extract(
            Context context,
            Uri source,
            File target,
            long maxDurationUs
    ) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        WavWriter writer = null;

        try {
            extractor.setDataSource(context, source, null);

            int audioTrack = -1;
            MediaFormat inputFormat = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat candidate = extractor.getTrackFormat(i);
                String mime = candidate.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrack = i;
                    inputFormat = candidate;
                    break;
                }
            }

            if (audioTrack < 0 || inputFormat == null) return false;

            extractor.selectTrack(audioTrack);
            String mime = inputFormat.getString(MediaFormat.KEY_MIME);
            if (mime == null) throw new IllegalStateException("Formato de áudio desconhecido");

            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(inputFormat, null, null, 0);
            decoder.start();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;
            long startedAtUs = -1L;

            while (!outputDone) {
                if (!inputDone) {
                    int inputIndex = decoder.dequeueInputBuffer(10_000);
                    if (inputIndex >= 0) {
                        ByteBuffer input = decoder.getInputBuffer(inputIndex);
                        if (input == null) {
                            throw new IllegalStateException("Buffer de áudio indisponível");
                        }
                        input.clear();

                        long sampleTime = extractor.getSampleTime();
                        if (startedAtUs < 0 && sampleTime >= 0) startedAtUs = sampleTime;

                        boolean durationReached =
                                sampleTime >= 0 &&
                                startedAtUs >= 0 &&
                                maxDurationUs > 0 &&
                                sampleTime - startedAtUs >= maxDurationUs;

                        int size = durationReached ? -1 : extractor.readSampleData(input, 0);
                        if (size < 0) {
                            decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    Math.max(0, sampleTime),
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            );
                            inputDone = true;
                        } else {
                            decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    size,
                                    sampleTime,
                                    extractor.getSampleFlags()
                            );
                            extractor.advance();
                        }
                    }
                }

                int outputIndex = decoder.dequeueOutputBuffer(info, 10_000);
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outputFormat = decoder.getOutputFormat();
                    int sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int pcmEncoding = outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)
                            ? outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            : AudioFormat.ENCODING_PCM_16BIT;

                    writer = new WavWriter(
                            target,
                            sampleRate,
                            channels,
                            pcmEncoding
                    );
                } else if (outputIndex >= 0) {
                    ByteBuffer output = decoder.getOutputBuffer(outputIndex);
                    if (output != null && info.size > 0) {
                        if (writer == null) {
                            MediaFormat outputFormat = decoder.getOutputFormat();
                            int sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                            int channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                            int pcmEncoding = outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)
                                    ? outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                    : AudioFormat.ENCODING_PCM_16BIT;
                            writer = new WavWriter(
                                    target,
                                    sampleRate,
                                    channels,
                                    pcmEncoding
                            );
                        }
                        writer.write(output, info.offset, info.size);
                    }

                    outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    decoder.releaseOutputBuffer(outputIndex, false);
                }
            }

            if (writer == null) return false;
            writer.close();
            writer = null;
            return target.isFile() && target.length() > 44;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
            if (decoder != null) {
                try {
                    decoder.stop();
                } catch (Exception ignored) {
                }
                try {
                    decoder.release();
                } catch (Exception ignored) {
                }
            }
            try {
                extractor.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static final class WavWriter implements AutoCloseable {
        private final RandomAccessFile file;
        private final int sourceRate;
        private final int channels;
        private final int pcmEncoding;
        private final double sourceFramesPerOutputFrame;
        private final byte[] buffer = new byte[64 * 1024];

        private int buffered = 0;
        private long dataBytes = 0;
        private long sourceFrameIndex = 0;
        private double nextOutputAtSourceFrame = 0.0;
        private boolean closed = false;

        WavWriter(
                File target,
                int sourceRate,
                int channels,
                int pcmEncoding
        ) throws Exception {
            if (sourceRate <= 0 || channels <= 0) {
                throw new IllegalArgumentException("Formato PCM inválido");
            }
            this.sourceRate = sourceRate;
            this.channels = channels;
            this.pcmEncoding = pcmEncoding;
            this.sourceFramesPerOutputFrame =
                    sourceRate / (double) TARGET_SAMPLE_RATE;

            file = new RandomAccessFile(target, "rw");
            file.setLength(0);
            file.write(new byte[44]);
        }

        void write(ByteBuffer source, int offset, int size) throws Exception {
            ByteBuffer data = source.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            data.position(offset);
            data.limit(offset + size);

            int bytesPerSample;
            if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                bytesPerSample = 4;
            } else if (pcmEncoding == AudioFormat.ENCODING_PCM_8BIT) {
                bytesPerSample = 1;
            } else {
                bytesPerSample = 2;
            }

            int frameBytes = bytesPerSample * channels;
            int frames = data.remaining() / frameBytes;

            for (int frame = 0; frame < frames; frame++) {
                long sum = 0;
                for (int channel = 0; channel < channels; channel++) {
                    int sample;
                    if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                        float value = data.getFloat();
                        value = Math.max(-1f, Math.min(1f, value));
                        sample = Math.round(value * 32767f);
                    } else if (pcmEncoding == AudioFormat.ENCODING_PCM_8BIT) {
                        sample = ((data.get() & 0xff) - 128) << 8;
                    } else {
                        sample = data.getShort();
                    }
                    sum += sample;
                }

                int mono = (int) (sum / channels);
                while (nextOutputAtSourceFrame <= sourceFrameIndex + 0.000001) {
                    writeSample((short) Math.max(
                            Short.MIN_VALUE,
                            Math.min(Short.MAX_VALUE, mono)
                    ));
                    nextOutputAtSourceFrame += sourceFramesPerOutputFrame;
                }
                sourceFrameIndex++;
            }
        }

        private void writeSample(short sample) throws Exception {
            if (buffered + 2 > buffer.length) flushBuffer();
            buffer[buffered++] = (byte) (sample & 0xff);
            buffer[buffered++] = (byte) ((sample >>> 8) & 0xff);
            dataBytes += 2;
        }

        private void flushBuffer() throws Exception {
            if (buffered <= 0) return;
            file.write(buffer, 0, buffered);
            buffered = 0;
        }

        @Override
        public void close() throws Exception {
            if (closed) return;
            closed = true;
            flushBuffer();
            writeHeader();
            file.close();
        }

        private void writeHeader() throws Exception {
            long byteRate = TARGET_SAMPLE_RATE * 2L;
            long riffSize = 36L + dataBytes;

            file.seek(0);
            writeAscii("RIFF");
            writeLeInt((int) riffSize);
            writeAscii("WAVE");
            writeAscii("fmt ");
            writeLeInt(16);
            writeLeShort((short) 1);
            writeLeShort((short) 1);
            writeLeInt(TARGET_SAMPLE_RATE);
            writeLeInt((int) byteRate);
            writeLeShort((short) 2);
            writeLeShort((short) 16);
            writeAscii("data");
            writeLeInt((int) dataBytes);
        }

        private void writeAscii(String value) throws Exception {
            file.writeBytes(value);
        }

        private void writeLeShort(short value) throws Exception {
            file.write(value & 0xff);
            file.write((value >>> 8) & 0xff);
        }

        private void writeLeInt(int value) throws Exception {
            file.write(value & 0xff);
            file.write((value >>> 8) & 0xff);
            file.write((value >>> 16) & 0xff);
            file.write((value >>> 24) & 0xff);
        }
    }
}
