package com.magmaguy.cannonrtp.config.contentpackages;

import com.magmaguy.cannonrtp.content.WorldCannonPackage;
import com.magmaguy.magmacore.config.CustomConfig;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

public class ContentPackageConfig extends CustomConfig {
    @Getter
    private static final Map<String, ContentPackageConfigFields> contentPackages = new LinkedHashMap<>();

    public ContentPackageConfig() {
        super("content_packages", "com.magmaguy.cannonrtp.config.contentpackages.premade", ContentPackageConfigFields.class);
        contentPackages.clear();
        for (Map.Entry<String, ? extends com.magmaguy.magmacore.config.CustomConfigFields> entry : super.getCustomConfigFieldsHashMap().entrySet()) {
            ContentPackageConfigFields fields = (ContentPackageConfigFields) entry.getValue();
            contentPackages.put(entry.getKey(), fields);
            new WorldCannonPackage(fields);
        }
    }
}
