package com.magmaguy.cannonrtp.config.contentpackages;

import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ContentPackageConfigFields extends CustomConfigFields {
    @Getter
    private int version = 0;
    @Getter
    private String name;
    @Getter
    private List<String> description;
    @Getter
    private String downloadLink;
    @Getter
    private String folderName;
    @Getter
    @Setter
    private String nightbreakSlug;
    @Getter
    @Setter
    private List<String> contentFilePrefixes;

    public ContentPackageConfigFields(String filename,
                                      boolean isEnabled,
                                      String name,
                                      List<String> description,
                                      String downloadLink,
                                      String folderName) {
        super(filename, isEnabled);
        this.name = name;
        this.description = description;
        this.downloadLink = downloadLink;
        this.folderName = folderName;
        this.contentFilePrefixes = List.of(folderName);
    }

    public ContentPackageConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        this.isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        this.name = processString("name", name, null, true);
        this.description = processStringList("description", description, null, true);
        this.downloadLink = processString("downloadLink", downloadLink, downloadLink, false);
        this.version = processInt("version", version, 0, true);
        this.folderName = processString("folderName", folderName, null, true);
        this.nightbreakSlug = processString("nightbreakSlug", nightbreakSlug, null, false);
        if (folderName == null) {
            Logger.warn("Content package config " + filename + " is missing folderName; defaulting contentFilePrefixes to an empty list.");
        }
        this.contentFilePrefixes = processStringList("contentFilePrefixes", contentFilePrefixes,
                folderName == null ? List.of() : List.of(folderName), false);
    }
}
