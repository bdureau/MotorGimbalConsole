package com.motorgimbalconsole;


import static processing.core.PApplet.asin;
import static processing.core.PApplet.atan;
import static processing.core.PApplet.atan2;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PApplet.sqrt;
import static processing.core.PApplet.unhex;

public class QuaternionUtils {
    float decodeFloat(String inString) {
        byte[] inData = new byte[4];

        if (inString.length() == 8) {
            inData[0] = (byte) unhex(inString.substring(0, 2));
            inData[1] = (byte) unhex(inString.substring(2, 4));
            inData[2] = (byte) unhex(inString.substring(4, 6));
            inData[3] = (byte) unhex(inString.substring(6, 8));
        }

        int intbits = (inData[3] << 24) | ((inData[2] & 0xff) << 16) | ((inData[1] & 0xff) << 8) | (inData[0] & 0xff);
        return Float.intBitsToFloat(intbits);
    }
    /*
Get Euler angles from quaternion
 */
    float [] quaternionToEuler(float[] q) {
        float euler[] = new float[3];
        euler[0] = atan2(2 * q[1] * q[2] - 2 * q[0] * q[3], 2 * q[0] * q[0] + 2 * q[1] * q[1] - 1); // psi
        euler[1] = -asin(2 * q[1] * q[3] + 2 * q[0] * q[2]); // theta
        euler[2] = atan2(2 * q[2] * q[3] - 2 * q[0] * q[1], 2 * q[0] * q[0] + 2 * q[3] * q[3] - 1); // phi
        return euler;
    }

    float [] quaternionToGravity(float[] q) {
        float [] gravity = new float[3];
        gravity[0]= 2 * (q[1]*q[3]-q[0]*q[0]);
        gravity[1]= 2 * (q[0]*q[1]+q[2]*q[3]);
        gravity[2]= q[0]*q[0]-q[1]*q[1]-q[2]*q[2]+q[3]*q[3];
        return gravity;
    }

    float [] quaternionToYawPitchRoll(float [] q, float [] gravity) {
        float [] ypr= new float [3];
        //yaw
        ypr[0] = atan2(2*q[1]*q[2]-2*q[0]*q[3], 2 * q[0] * q[0] + 2 * q[1] * q[1] - 1);
        //pitch
        ypr[1] =atan(gravity[0]/sqrt(gravity[1]*gravity[1]+gravity[2]*gravity[2]));
        //roll
        ypr[2] =atan(gravity[1]/sqrt(gravity[0]*gravity[0]+gravity[2]*gravity[2]));
        return ypr;
    }

    float[] quatProd(float[] a, float[] b) {
        float[] q = new float[4];

        q[0] = a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3];
        q[1] = a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2];
        q[2] = a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1];
        q[3] = a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0];
        return q;
    }

    // returns a quaternion from an axis angle representation
    float[] quatAxisAngle(float[] axis, float angle) {
        float[] q = new float[4];

        float halfAngle = (float) (angle / 2.0);
        float sinHalfAngle = sin(halfAngle);
        q[0] = cos(halfAngle);
        q[1] = -axis[0] * sinHalfAngle;
        q[2] = -axis[1] * sinHalfAngle;
        q[3] = -axis[2] * sinHalfAngle;

        return q;
    }

    // return the quaternion conjugate of quat
    float[] quatConjugate(float[] quat) {
        float[] conj = new float[4];

        conj[0] = quat[0];
        conj[1] = -quat[1];
        conj[2] = -quat[2];
        conj[3] = -quat[3];

        return conj;
    }

}
