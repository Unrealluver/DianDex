package com.dian.variant

import org.gradle.api.Project

class DianDexBuilder {
    DianDexVariant dianDexVariant
    Project project
    String variantName

    DianDexBuilder(DianDexVariant dianDexVariant){
        this.dianDexVariant = dianDexVariant
    }


}