package com.motorgimbalconsole.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

/**
 * @description: Gimbal rocket 3D attitude renderer using Processing Android.
 * Renders the rocket with exact geometric alignment (nose cone and nozzle flush with fuselage),
 * roll-detection checkerboard texture, and original avionics rotation axes.
 *
 * @author: boris.dureau@neuf.fr
 */
public class Rocket extends PApplet {

    private static final String TAG = "Rocket3D";

    // Attitude variables (in radians)
    private final float[] q = new float[4];
    private float[] hq = null;
    private final float[] Euler = new float[3];     // Euler[0]=X, Euler[1]=Y, Euler[2]=Z
    private final float[] RealEuler = new float[3];
    private final float[] Gravity = new float[3];
    private final float[] YPR = new float[3];

    private long currentTime = 0;
    private float correct = 0;
    private float servoX = 0, servoY = 0;
    private boolean useQuaternion = true;

    private PFont font;
    private PImage img;

    // Viewport dimensions from original code
    private final int VIEW_SIZE_X = 1200;
    private final int VIEW_SIZE_Y = 1200;

    // Fuselage geometry specifications
    private final float BODY_RADIUS = 50.0f;
    private final float BODY_HEIGHT = 500.0f;       // Extends from Z = -250 to Z = +250
    private final float CONE_HEIGHT = 150.0f;       // Extends from Z = -75 to Z = +75 (Apex at -75)
    private final float NOZZLE_HEIGHT = 50.0f;      // Extends from Z = 0 to Z = +50

    @Override
    public void settings() {
        size(VIEW_SIZE_X, VIEW_SIZE_Y, P3D);
    }

    @Override
    public void setup() {
        frameRate(60);
        smooth(4);

        // Safe font loading
        try {
            font = loadFont("CourierNew36.vlw");
        } catch (Exception e) {
            font = null;
        }

        // Texture loading with automatic fallback if pattern.png is absent
        try {
            img = loadImage("pattern.png");
        } catch (Exception e) {
            img = null;
        }

        if (img == null) {
            img = generateCheckerboardTexture(256, 256, 8);
        }
    }

    /**
     * Generates a high-contrast checkerboard texture if pattern.png is missing.
     */
    private PImage generateCheckerboardTexture(int w, int h, int tiles) {
        PImage tex = createImage(w, h, RGB);
        tex.loadPixels();
        int tileSize = w / tiles;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean isWhite = ((x / tileSize) + (y / tileSize)) % 2 == 0;
                tex.pixels[y * w + x] = isWhite ? color(245, 245, 248) : color(20, 22, 26);
            }
        }
        tex.updatePixels();
        return tex;
    }

    /**
     * Strictly matches the original mapping:
     * Euler[0] = X (deg to rad)
     * Euler[1] = Y (deg to rad)
     * Euler[2] = Z (deg to rad)
     */
    public synchronized void setInputString(float X, float Y, float Z, long time) {
        Euler[2] = Z * (PI / 180.0f);
        Euler[1] = Y * (PI / 180.0f);
        Euler[0] = X * (PI / 180.0f);
        currentTime = time;
        useQuaternion = false;
    }

    public synchronized void setInputString(String inputString) {
        String[] inputStringArr = split(inputString, ",");
        if (inputStringArr.length >= 4) {
            q[0] = decodeFloat(inputStringArr[0]);
            q[1] = decodeFloat(inputStringArr[1]);
            q[2] = decodeFloat(inputStringArr[2]);
            q[3] = decodeFloat(inputStringArr[3]);
        }
        useQuaternion = true;
    }

    @SuppressLint("LongLogTag")
    public void setInputCorrect(String inputString) {
        if (inputString != null && inputString.matches("-?\\d+(?:\\.\\d+)?")) {
            correct = Float.parseFloat(inputString);
        } else {
            Log.d("Rocket - setInputCorrect", String.valueOf(inputString));
        }
    }

    public void setServoX(String inputString) {
        if (inputString != null && inputString.matches("-?\\d+(?:\\.\\d+)?")) {
            servoX = Float.parseFloat(inputString);
        }
    }

    public void setServoY(String inputString) {
        if (inputString != null && inputString.matches("-?\\d+(?:\\.\\d+)?")) {
            servoY = Float.parseFloat(inputString);
        }
    }

    private float decodeFloat(String inString) {
        if (inString == null || inString.length() != 8) return 0.0f;
        byte[] inData = new byte[4];
        inData[0] = (byte) unhex(inString.substring(0, 2));
        inData[1] = (byte) unhex(inString.substring(2, 4));
        inData[2] = (byte) unhex(inString.substring(4, 6));
        inData[3] = (byte) unhex(inString.substring(6, 8));
        int intbits = (inData[3] << 24) | ((inData[2] & 0xff) << 16) | ((inData[1] & 0xff) << 8) | (inData[0] & 0xff);
        return Float.intBitsToFloat(intbits);
    }

    @Override
    public void draw() {
        // Futuristic telemetry dark background
        background(15, 18, 25);

        // Ambient and directional lighting for 3D depth
        ambientLight(80, 85, 100);
        directionalLight(255, 255, 255, 0.5f, 0.8f, -1.0f);
        lightSpecular(180, 180, 200);

        if (useQuaternion) {
            if (hq != null) {
                quaternionToEuler(quatProd(hq, q), Euler);
            } else {
                quaternionToEuler(q, Euler);
            }
            quaternionToEuler(q, RealEuler);
            quaternionToGravity(q, Gravity);
            quaternionToYawPitchRoll(q, Gravity, YPR);
        }

        drawRotatingRocket();
        drawTelemetryHUD();
    }

    /**
     * Replicates original rotation chain:
     * translate -> rotateZ(-Euler[2]) -> rotateX(-Euler[1]-correct) -> rotateY(-Euler[0]) -> drawRocket()
     */
    private void drawRotatingRocket() {
        pushMatrix();
        translate(width / 2.0f, height / 2.0f + 40, 0);

        rotateZ(-Euler[2]);
        rotateX(-Euler[1] - radians(correct));
        rotateY(-Euler[0]);

        drawRocket();
        popMatrix();
    }

    /**
     * Draws the rocket assembly. Restores the essential rotateY(PI / 2) from original code.
     */
    private void drawRocket() {
        // Essential alignment from original Rocket.java
        rotateY(PI / 2);

        // Attitude reference ring around Center of Gravity
        noFill();
        stroke(0, 180, 255, 60);
        strokeWeight(1.8f);
        ellipse(0, 0, 220, 220);

        // 1. FUSELAGE (Cylinder centered at origin: extends from Z = -250 to Z = +250)
        pushMatrix();
        drawTextureCylinder(36, BODY_RADIUS, BODY_RADIUS, BODY_HEIGHT, img);
        popMatrix();

        // 2. NOSE CONE (Height = 150, centered at Z = -325 -> Base at -250, Apex at -400)
        // Joint is exactly flush at Z = -250
        pushMatrix();
        translate(0, 0, - (BODY_HEIGHT / 2.0f + CONE_HEIGHT / 2.0f)); // Z = -325
        drawTaperedCone(36, 0.0f, BODY_RADIUS, CONE_HEIGHT);
        popMatrix();

        // 3. NOZZLE (Starts exactly at fuselage bottom Z = +250 and extends backward)
        pushMatrix();
        translate(0, 0, BODY_HEIGHT / 2.0f); // Z = +250
        drawNozzle(24, 38.0f, 24.0f, NOZZLE_HEIGHT);
        popMatrix();

        // 4. FINS (Mounted symmetrically on the rear fuselage)
        drawFins();
    }

    /**
     * Draws the nose cone perfectly matching the cylinder base radius.
     * In local coordinates: apex is at -halfHeight, base is at +halfHeight.
     */
    private void drawTaperedCone(int sides, float rTop, float rBottom, float h) {
        float angle = 360.0f / sides;
        float halfHeight = h / 2.0f;

        noStroke();
        fill(225, 45, 35); // Aerospace Red

        // Conical surface
        beginShape(TRIANGLE_FAN);
        vertex(0, 0, -halfHeight); // Apex tip
        for (int i = 0; i <= sides; i++) {
            float x = cos(radians(i * angle)) * rBottom;
            float y = sin(radians(i * angle)) * rBottom;
            vertex(x, y, halfHeight);
        }
        endShape(CLOSE);

        // Flat base cap (at the junction)
        beginShape();
        for (int i = 0; i < sides; i++) {
            float x = cos(radians(i * angle)) * rBottom;
            float y = sin(radians(i * angle)) * rBottom;
            vertex(x, y, halfHeight);
        }
        endShape(CLOSE);
    }

    /**
     * Draws the motor nozzle extending backward from the rear bulkhead.
     */
    private void drawNozzle(int sides, float rTop, float rBottom, float h) {
        float angle = 360.0f / sides;
        noStroke();
        fill(55, 60, 68); // Dark Titanium

        beginShape(QUAD_STRIP);
        for (int i = 0; i <= sides; i++) {
            float x1 = cos(radians(i * angle)) * rTop;
            float y1 = sin(radians(i * angle)) * rTop;
            float x2 = cos(radians(i * angle)) * rBottom;
            float y2 = sin(radians(i * angle)) * rBottom;
            vertex(x1, y1, 0);
            vertex(x2, y2, h);
        }
        endShape(CLOSE);
    }

    /**
     * Draws 4 beveled stabilizing fins positioned at 90° angles around the base.
     */
    private void drawFins() {
        fill(225, 45, 35);
        stroke(20, 22, 28);
        strokeWeight(1.2f);

        for (int i = 0; i < 4; i++) {
            pushMatrix();
            rotateZ(HALF_PI * i);

            // Trailing edge at Z = +250 (bottom of fuselage)
            beginShape();
            vertex(BODY_RADIUS, 0, 100);       // Root leading edge
            vertex(BODY_RADIUS + 55, 0, 210);  // Tip leading edge
            vertex(BODY_RADIUS + 55, 0, 250);  // Tip trailing edge
            vertex(BODY_RADIUS, 0, 245);       // Root trailing edge
            endShape(CLOSE);

            popMatrix();
        }
    }

    /**
     * Draws the textured cylinder with the checkerboard pattern along Z axis.
     */
    private void drawTextureCylinder(int sides, float r1, float r2, float h, PImage image) {
        float angle = 360.0f / sides;
        float halfHeight = h / 2.0f;

        // Top cap (at Z = -halfHeight)
        beginShape();
        fill(230);
        noStroke();
        for (int i = 0; i < sides; i++) {
            float x = cos(radians(i * angle)) * r1;
            float y = sin(radians(i * angle)) * r1;
            vertex(x, y, -halfHeight);
        }
        endShape(CLOSE);

        // Bottom cap (at Z = +halfHeight)
        beginShape();
        fill(45);
        noStroke();
        for (int i = 0; i < sides; i++) {
            float x = cos(radians(i * angle)) * r2;
            float y = sin(radians(i * angle)) * r2;
            vertex(x, y, halfHeight);
        }
        endShape(CLOSE);

        // Lateral body surface with texture
        beginShape(QUAD_STRIP);
        noStroke();
        if (image != null) {
            texture(image);
        } else {
            fill(240);
        }

        for (int i = 0; i <= sides; i++) {
            float x1 = cos(radians(i * angle)) * r1;
            float y1 = sin(radians(i * angle)) * r1;
            float x2 = cos(radians(i * angle)) * r2;
            float y2 = sin(radians(i * angle)) * r2;
            float u = (image != null) ? (image.width / (float) sides) * i : 0;

            vertex(x1, y1, -halfHeight, u, 0);
            vertex(x2, y2, halfHeight, u, (image != null) ? image.height : 0);
        }
        endShape(CLOSE);
    }

    /**
     * 2D Overlay HUD.
     */
    private void drawTelemetryHUD() {
        hint(DISABLE_DEPTH_TEST);
        camera();

        if (font != null) {
            textFont(font);
        }
        textSize(16);

        // Background header strip
        noStroke();
        fill(10, 14, 20, 200);
        rect(16, 16, width - 32, 52, 10);

        fill(0, 215, 255);
        textAlign(LEFT, CENTER);
        text(String.format("T+ %.2f s", currentTime / 1000.0f), 32, 42);

        fill(255);
        textAlign(RIGHT, CENTER);
        text(String.format("X: %+.1f°   Y: %+.1f°   Z: %+.1f°",
                degrees(Euler[0]), degrees(Euler[1]), degrees(Euler[2])), width - 32, 42);

        hint(ENABLE_DEPTH_TEST);
    }

    // Quaternion math routines
    void quaternionToEuler(float[] q, float[] euler) {
        euler[0] = atan2(2 * q[1] * q[2] - 2 * q[0] * q[3], 2 * q[0] * q[0] + 2 * q[1] * q[1] - 1);
        euler[1] = -asin(2 * q[1] * q[3] + 2 * q[0] * q[2]);
        euler[2] = atan2(2 * q[2] * q[3] - 2 * q[0] * q[1], 2 * q[0] * q[0] + 2 * q[3] * q[3] - 1);
    }

    void quaternionToGravity(float[] q, float[] gravity) {
        gravity[0] = 2 * (q[1] * q[3] - q[0] * q[2]);
        gravity[1] = 2 * (q[0] * q[1] + q[2] * q[3]);
        gravity[2] = q[0] * q[0] - q[1] * q[1] - q[2] * q[2] + q[3] * q[3];
    }

    void quaternionToYawPitchRoll(float[] q, float[] gravity, float[] ypr) {
        ypr[0] = atan2(2 * q[1] * q[2] - 2 * q[0] * q[3], 2 * q[0] * q[0] + 2 * q[1] * q[1] - 1);
        ypr[1] = atan(gravity[0] / sqrt(gravity[1] * gravity[1] + gravity[2] * gravity[2]));
        ypr[2] = atan(gravity[1] / sqrt(gravity[0] * gravity[0] + gravity[2] * gravity[2]));
    }

    float[] quatProd(float[] a, float[] b) {
        float[] res = new float[4];
        res[0] = a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3];
        res[1] = a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2];
        res[2] = a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1];
        res[3] = a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0];
        return res;
    }
}