package com.aurora.services.patch;

import com.aurora.services.patch.IProgressCallback;

interface IMorpheService {
    boolean startPatch(in String packageName, in String apksDir, in String outputFile, in IProgressCallback callback);
}