package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.interfaces.MediaVault;
import com.coduel.model.constant.Errors;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Media business logic over the {@link MediaVault} storage port (the same way RoomApi sits over
 * RoomRegistry). Validates an upload and downscales + re-encodes it so the stored size is bounded
 * regardless of the source (a 12 MP photo → ~100–300 KB), then hands the bytes to the port. Not
 * @Transactional — there's no DB work, and we must not hold a connection during CPU-bound image work.
 */
@Service
public class MediaApi extends AbstractApi {

    private static final long MAX_BYTES = 6 * 1024 * 1024; // hard guard before we spend CPU decoding
    private static final int MAX_EDGE = 1600;              // longest side cap — plenty for chat
    private static final double JPEG_QUALITY = 0.82;
    private static final long MAX_AUDIO_BYTES = 12 * 1024 * 1024; // voice notes are short; generous cap
    // Allowed content types → the extension we save under (we control the name, not the client).
    private static final Map<String, String> ALLOWED = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/gif", "gif",
            "image/webp", "webp");
    // Allowed voice-note audio types → stored extension. webm/ogg are what MediaRecorder produces.
    private static final Map<String, String> ALLOWED_AUDIO = Map.of(
            "audio/webm", "webm",
            "audio/ogg", "ogg",
            "audio/mpeg", "mp3",
            "audio/mp4", "m4a",
            "audio/wav", "wav");
    private static final String IMAGE_FOLDER = "chat";
    private static final String VOICE_FOLDER = "voice";

    @Autowired
    private MediaVault mediaVault;

    public String storeImage(MultipartFile file) throws ApiException {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_143, List.of());
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_144, List.of(MAX_BYTES / (1024 * 1024)));
        }
        String ext = ALLOWED.get(file.getContentType());
        if (Objects.isNull(ext)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_145, List.of());
        }
        try {
            byte[] original = file.getBytes();
            // Animated GIF / WebP can't be safely re-encoded with ImageIO (flattens animation / no WebP
            // writer), so store them as-is — already bounded by the size guard above.
            if (ext.equals("gif") || ext.equals("webp")) {
                return mediaVault.store(original, ext, IMAGE_FOLDER);
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
            if (Objects.isNull(image)) {
                throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_145, List.of()); // not a decodable image
            }
            // PNG (screenshots/code) stays PNG to keep text crisp + transparency; photos go JPEG @ q0.82.
            String outFormat = ext.equals("png") ? "png" : "jpg";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.Builder<BufferedImage> builder =
                    Thumbnails.of(image).outputFormat(outFormat).outputQuality(JPEG_QUALITY);
            if (image.getWidth() > MAX_EDGE || image.getHeight() > MAX_EDGE) {
                builder.size(MAX_EDGE, MAX_EDGE).keepAspectRatio(true);
            } else {
                builder.scale(1.0); // already small — re-encode (strips EXIF) without upscaling
            }
            builder.toOutputStream(out);
            return mediaVault.store(out.toByteArray(), outFormat, IMAGE_FOLDER);
        } catch (IOException e) {
            throw new ApiException(ApiStatus.UNKNOWN_ERROR, Errors.ERR_146, List.of());
        }
    }

    // Store a voice note as-is (no processing — it's already a compact compressed audio blob). Validated
    // for type + size, then handed to the vault under the "voice" folder.
    public String storeAudio(MultipartFile file) throws ApiException {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_143, List.of());
        }
        if (file.getSize() > MAX_AUDIO_BYTES) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_144, List.of(MAX_AUDIO_BYTES / (1024 * 1024)));
        }
        // Browsers tag MediaRecorder blobs as e.g. "audio/webm;codecs=opus" — match on the base type.
        String contentType = Objects.isNull(file.getContentType()) ? "" : file.getContentType().split(";")[0].trim();
        String ext = ALLOWED_AUDIO.get(contentType);
        if (Objects.isNull(ext)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_149, List.of());
        }
        try {
            return mediaVault.store(file.getBytes(), ext, VOICE_FOLDER);
        } catch (IOException e) {
            throw new ApiException(ApiStatus.UNKNOWN_ERROR, Errors.ERR_146, List.of());
        }
    }
}
