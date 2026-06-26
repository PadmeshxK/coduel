package com.coduel.media;

import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.config.AppProperties;
import com.coduel.interfaces.MediaVault;
import com.coduel.model.constant.Errors;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Local-filesystem {@link MediaVault} (dev) — pure storage. Writes the (already validated + processed)
 * bytes under a random UUID name and returns a public, capability-style URL (/uploads/{random}.ext);
 * the unguessable name is the access token, so files are served without auth. Prod can replace this with
 * an S3/R2 adapter behind the same port (active by default, or when media.storage=local).
 */
@Component
@ConditionalOnProperty(name = "media.storage", havingValue = "local", matchIfMissing = true)
@Log4j2
public class LocalMediaVault implements MediaVault {

    @Autowired
    private AppProperties appProperties;

    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Paths.get(appProperties.getUploadsDir());
        Files.createDirectories(root);
        log.info("Media uploads dir: {}", root.toAbsolutePath());
    }

    @Override
    public String store(byte[] data, String extension, String folder) throws ApiException {
        String name = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Path target = root.resolve(name);
            Files.createDirectories(target.getParent()); // the folder subdir, created on first use
            Files.write(target, data);
        } catch (IOException e) {
            log.error("Failed to store upload: {}", e.getMessage());
            throw new ApiException(ApiStatus.UNKNOWN_ERROR, Errors.ERR_146, List.of());
        }
        return "/uploads/" + name;
    }
}
