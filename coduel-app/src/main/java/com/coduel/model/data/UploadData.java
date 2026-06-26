package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** Result of an image upload — the stored media URL the client then sends as a message attachment. */
@Getter
@Setter
public class UploadData {

    private String url;
}
