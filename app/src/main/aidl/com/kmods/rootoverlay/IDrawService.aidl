// IDrawService.aidl
package com.kmods.rootoverlay;

// Declare any non-default types here with import statements

interface IDrawService {
    int getPid();
    int getUid();
    void initSurface(inout Surface surface);
    void updateSurface(int width, int height);
    void setOrientation(int orientation);
}