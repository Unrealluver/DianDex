package com.dian.util

import com.dian.variant.DianDexVariant
import org.gradle.api.Project

class DianDexInstantRun {
    DianDexVariant dianDexVariant
    Project project
    File resourceApFile

    DianDexInstantRun(DianDexVariant dianDexVariant){
        this.dianDexVariant = dianDexVariant
        this.project = dianDexVariant.project
    }
}