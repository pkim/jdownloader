package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.AudioMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.FFMpegInfoReader;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg.Stream;

public class AudioHandler extends ExtensionHandler<AudioMediaItem> {

    @Override
    public AudioMediaItem handle(StreamingExtension extension, DownloadLink dl) {
        AudioMediaItem ret = new AudioMediaItem(dl);
        if (CrossSystem.isWindows()) {
            try {

                FFMpegInfoReader ffmpeg = new FFMpegInfoReader(dl);

                ffmpeg.load(extension);
                boolean hasVideoStream = false;
                ArrayList<Stream> streams = ffmpeg.getStreams();
                if (streams != null) {
                    for (Stream info : streams) {
                        // mjpeg = mp3 covers
                        if ("video".equals(info.getCodec_type()) && !"mjpeg".equalsIgnoreCase(info.getCodec_name())) {
                            return null;
                        } else if ("audio".equals(info.getCodec_type())) {

                            ret.setStream(info.toAudioStream());
                            break;
                        }

                    }
                    ret.setThumbnailPath(Files.getRelativePath(Application.getResource("tmp").getParentFile(), new File(ffmpeg.getThumbnailPath())));
                    ret.setContainerFormat(ffmpeg.getFormat().getFormat_name());
                    ret.setSize(ffmpeg.getFormat().parseSize());
                    if (ffmpeg.getFormat().getTags() != null) {
                        ret.setTitle(ffmpeg.getFormat().getTags().getTitle());
                        ret.setAlbum(ffmpeg.getFormat().getTags().getAlbum());
                        ret.setArtist(ffmpeg.getFormat().getTags().getArtist());
                    }

                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return ret;
    }

}