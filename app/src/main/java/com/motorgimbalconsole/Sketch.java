package com.motorgimbalconsole;

import java.io.IOException;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

public class Sketch extends PApplet {
    public void settings() {
        //size(800, 600);
        size(1000, 1000, P3D);
    }

   float [] q = new float [4];
   float [] hq = null;
   float [] Euler = new float [3]; // psi, theta, phi

   int lf = 10; // 10 is '\n' in ASCII
    long run =0;

   byte[] inBuffer = new byte[22]; // this is the number of chars on each line from the Arduino (including /r/n)
   ConsoleApplication myBT;
   PFont font;
   //PImage topside,downside,frontside,rightside,backside,leftside;
    PImage img;
    final int VIEW_SIZE_X = 1000, VIEW_SIZE_Y = 1000;

    public void setMyApp(ConsoleApplication bt) {
        myBT= bt;
    }

   public void setup()
   {
       fill(255);
       stroke(color(44,48,32));

       frameRate(400);


       // The font must be located in the sketch's "data" directory to load successfully
      font = loadFont("CourierNew36.vlw");

      // Loading the textures to the rocket
       img =loadImage("pattern.png");

      delay(100);

   }
    float decodeFloat(String inString) {
        byte [] inData = new byte[4];

        if(inString.length() == 8) {
            inData[0] = (byte) unhex(inString.substring(0, 2));
            inData[1] = (byte) unhex(inString.substring(2, 4));
            inData[2] = (byte) unhex(inString.substring(4, 6));
            inData[3] = (byte) unhex(inString.substring(6, 8));
        }

        int intbits = (inData[3] << 24) | ((inData[2] & 0xff) << 16) | ((inData[1] & 0xff) << 8) | (inData[0] & 0xff);
        return Float.intBitsToFloat(intbits);
    }


    void readQ() {

        try {
            if(myBT.getInputStream().available() >18) {

                String inputString="";
                char ch='\0';
                while (ch != '\n') {
                    // this is not the end of our command
                    ch = (char) myBT.getInputStream().read();
                    if(ch!='\n')
                        inputString=inputString+Character.toString(ch);
                }

                if (inputString != null && inputString.length() > 0) {
                    String [] inputStringArr = split(inputString, ",");
                    if(inputStringArr.length >= 5) { // q1,q2,q3,q4,\r\n so we have 5 elements
                        q[0] = decodeFloat(inputStringArr[0]);
                        q[1] = decodeFloat(inputStringArr[1]);
                        q[2] = decodeFloat(inputStringArr[2]);
                        q[3] = decodeFloat(inputStringArr[3]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            text("We have problems", 20, VIEW_SIZE_Y - 30);
        }
    }




void drawRotatingRocket() {
    pushMatrix();
    translate(VIEW_SIZE_X/2, VIEW_SIZE_Y/2 + 50, 0);
    rotateZ(-Euler[2]);
    rotateX(-Euler[1]);
    rotateY(-Euler[0]);
    drawRocket();
    popMatrix();
}
    public void draw() {
        background(0);
        //fill(#ffffff);
        if (myBT.getConnected()) {
            for (int i = 0; i < 100 + 1; ++i) {
               readQ();
            }
            //run++;
            //text("Connected:"+run, 20, VIEW_SIZE_Y - 30);
        }
        else {
            text("DisConnected:"+run, 20, VIEW_SIZE_Y - 30);
        }

        if(hq != null) { // use home quaternion
            quaternionToEuler(quatProd(hq, q), Euler);
            text("Disable home position by pressing \"n\"", 20, VIEW_SIZE_Y - 30);
        }
        else {
            quaternionToEuler(q, Euler);
            //text("Point FreeIMU's X axis to your monitor then press \"h\"", 20, VIEW_SIZE_Y - 30);
        }

        textFont(font, 20);
        textAlign(LEFT, TOP);
        text("Q:\n" + q[0] + "\n" + q[1] + "\n" + q[2] + "\n" + q[3], 20, 20);
        text("Euler Angles:\nYaw (psi)  : " + degrees(Euler[0]) + "\nPitch (theta): " + degrees(Euler[1]) + "\nRoll (phi)  : " + degrees(Euler[2]), 200, 20);

        drawRotatingRocket();
    }



  public void setOrientation (char key) {

      if(key == 'h') {
          // set hq the home quaternion as the quatnion conjugate coming from the sensor fusion
          hq = quatConjugate(q);
      }
      else if(key == 'n') {
          hq = null;
      }
  }

// See Sebastian O.H. Madwick report
// "An efficient orientation filter for inertial and intertial/magnetic sensor arrays" Chapter 2 Quaternion representation

    void quaternionToEuler(float [] q, float [] euler) {
        euler[0] = atan2(2 * q[1] * q[2] - 2 * q[0] * q[3], 2 * q[0]*q[0] + 2 * q[1] * q[1] - 1); // psi
        euler[1] = -asin(2 * q[1] * q[3] + 2 * q[0] * q[2]); // theta
        euler[2] = atan2(2 * q[2] * q[3] - 2 * q[0] * q[1], 2 * q[0] * q[0] + 2 * q[3] * q[3] - 1); // phi
    }

    float [] quatProd(float [] a, float [] b) {
        float [] q = new float[4];

        q[0] = a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3];
        q[1] = a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2];
        q[2] = a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1];
        q[3] = a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0];

        return q;
    }

    // returns a quaternion from an axis angle representation
    float [] quatAxisAngle(float [] axis, float angle) {
        float [] q = new float[4];

        float halfAngle = (float) (angle / 2.0);
        float sinHalfAngle = sin(halfAngle);
        q[0] = cos(halfAngle);
        q[1] = -axis[0] * sinHalfAngle;
        q[2] = -axis[1] * sinHalfAngle;
        q[3] = -axis[2] * sinHalfAngle;

        return q;
    }

    // return the quaternion conjugate of quat
    float [] quatConjugate(float [] quat) {
        float [] conj = new float[4];

        conj[0] = quat[0];
        conj[1] = -quat[1];
        conj[2] = -quat[2];
        conj[3] = -quat[3];

        return conj;
    }

    void drawRocket()
    {
        background(0, 128, 255);

        rotateY(PI/2);
        pushMatrix();

        drawTextureCylinder( 36, 50, 50, 500, img);
        popMatrix();
        pushMatrix();
        translate( 0, 0, -325);

        drawTextureCylinder( 36, 0, 50, 150, img);
        popMatrix();

        beginShape();
        //start rocket fin set
        //fill(255,255,255);
        translate( 0, 0, 100 );
        rotateY(PI);
        //rotateZ(PI/3);
        vertex(-100, 0, -150);
        vertex( 100, 0, -150);
        vertex(   0,    0,  100);
        endShape();

        //start another rocket fin set
        fill(0);
        translate( 0, 0, -150 );
        beginShape();
        vertex( 0, 100, 0);
        vertex( 0,  -100, 0);
        vertex(   0,    0,  250);
        endShape();

    }

    void drawTextureCylinder( int sides, float r1, float r2, float h, PImage image)
    {
        float angle = 360 / sides;
        float halfHeight = h / 2;

        // top
        beginShape();
        //texture(topside);
        for (int i = 0; i < sides; i++) {
            float x = cos( radians( i * angle ) ) * r1;
            float y = sin( radians( i * angle ) ) * r1;
            vertex( x, y, -halfHeight);
        }
        endShape(CLOSE);
        // bottom
        beginShape();
        //texture(topside);
        for (int i = 0; i < sides; i++) {
            float x = cos( radians( i * angle ) ) * r2;
            float y = sin( radians( i * angle ) ) * r2;
            vertex( x, y, halfHeight);
        }
        endShape(CLOSE);
        // draw body
        //beginShape(TRIANGLE_STRIP);
        beginShape(QUAD_STRIP);
        texture(image);
        for (int i = 0; i < sides + 1; i++) {
            float x1 = cos( radians( i * angle ) ) * r1;
            float y1 = sin( radians( i * angle ) ) * r1;
            float x2 = cos( radians( i * angle ) ) * r2;
            float y2 = sin( radians( i * angle ) ) * r2;
            float u = image.width / sides * i;
        /*vertex( x1, y1, -halfHeight);
        vertex( x2, y2, halfHeight);*/
            vertex( x1,  y1,-halfHeight,u,0);
            vertex( x2,  y2,halfHeight,u,image.height);
        }
        endShape(CLOSE);

    }
}
