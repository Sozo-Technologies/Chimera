package org.sozotech.system;

import org.sozotech.ml.preprocess.HandData;

public class FallbackDetector {

    private static final int WRIST      = 0;
    private static final int THUMB_CMC  = 1;
    private static final int THUMB_MCP  = 2;
    private static final int THUMB_IP   = 3;
    private static final int THUMB_TIP  = 4;
    private static final int INDEX_MCP  = 5;
    private static final int INDEX_PIP  = 6;
    private static final int INDEX_DIP  = 7;
    private static final int INDEX_TIP  = 8;
    private static final int MIDDLE_MCP = 9;
    private static final int MIDDLE_PIP = 10;
    private static final int MIDDLE_DIP = 11;
    private static final int MIDDLE_TIP = 12;
    private static final int RING_MCP   = 13;
    private static final int RING_PIP   = 14;
    private static final int RING_DIP   = 15;
    private static final int RING_TIP   = 16;
    private static final int PINKY_MCP  = 17;
    private static final int PINKY_PIP  = 18;
    private static final int PINKY_DIP  = 19;
    private static final int PINKY_TIP  = 20;

    private static final float VISIBILITY_THRESHOLD = 0.6f;

    private float[][] w;
    private float[][] img;
    private float[] vis;

    private float wx(int id) { return w[id][0]; }
    private float wy(int id) { return w[id][1]; }
    private float wz(int id) { return w[id][2]; }

    private boolean visible(int id) {
        return vis[id] >= VISIBILITY_THRESHOLD
                && w[id][0] != -1f
                && !Float.isNaN(w[id][0]);
    }

    private float[] vec(int from, int to) {
        return new float[]{
                wx(to) - wx(from),
                wy(to) - wy(from),
                wz(to) - wz(from)
        };
    }

    private float dot(float[] a, float[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    private float mag(float[] v) {
        return (float) Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    }

    private float angleBetween(float[] v1, float[] v2) {
        float m1 = mag(v1), m2 = mag(v2);
        if (m1 < 1e-7f || m2 < 1e-7f) return 0f;
        float cos = Math.max(-1f, Math.min(1f, dot(v1, v2) / (m1 * m2)));
        return (float) Math.toDegrees(Math.acos(cos));
    }

    private float jointAngle(int prev, int joint, int next) {
        if (!visible(prev) || !visible(joint) || !visible(next)) return 180f;
        return angleBetween(vec(joint, prev), vec(joint, next));
    }

    private float boneAngle(int fromA, int toA, int fromB, int toB) {
        if (!visible(fromA) || !visible(toA) || !visible(fromB) || !visible(toB)) return 0f;
        return angleBetween(vec(fromA, toA), vec(fromB, toB));
    }

    private float dist(int a, int b) {
        float dx = wx(a)-wx(b), dy = wy(a)-wy(b), dz = wz(a)-wz(b);
        return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    private float handScale() {
        return dist(WRIST, MIDDLE_MCP);
    }

    private boolean extended(int mcp, int pip, int dip, int tip) {
        float pip_angle = jointAngle(mcp, pip, dip);
        float dip_angle = jointAngle(pip, dip, tip);
        return pip_angle > 160f && dip_angle > 160f;
    }

    private boolean curled(int mcp, int pip, int dip, int tip) {
        float pip_angle = jointAngle(mcp, pip, dip);
        float dip_angle = jointAngle(pip, dip, tip);
        return pip_angle < 95f && dip_angle < 115f;
    }

    private boolean halfCurled(int mcp, int pip, int dip, int tip) {
        float pip_angle = jointAngle(mcp, pip, dip);
        float dip_angle = jointAngle(pip, dip, tip);
        return pip_angle >= 95f && pip_angle <= 165f
                && dip_angle >= 95f && dip_angle <= 165f;
    }

    private boolean hooked(int mcp, int pip, int dip, int tip) {
        float pip_angle = jointAngle(mcp, pip, dip);
        float dip_angle = jointAngle(pip, dip, tip);
        return pip_angle > 145f && dip_angle < 100f;
    }

    private boolean thumbAbducted() {
        if (!visible(THUMB_CMC) || !visible(THUMB_TIP) || !visible(INDEX_MCP)) return false;
        float ang = boneAngle(THUMB_CMC, THUMB_TIP, WRIST, INDEX_MCP);
        return ang > 38f;
    }

    private boolean thumbTouches(int tip) {
        if (!visible(THUMB_TIP) || !visible(tip)) return false;
        float scale = handScale();
        if (scale < 1e-7f) return false;
        return (dist(THUMB_TIP, tip) / scale) < 0.40f;
    }

    private boolean thumbTucked() {
        if (!visible(THUMB_TIP) || !visible(INDEX_MCP)) return false;
        float scale = handScale();
        return (wy(THUMB_TIP) - wy(INDEX_MCP)) / scale > -0.05f;
    }

    private boolean thumbBetweenIndexMiddle() {
        if (!visible(THUMB_TIP) || !visible(INDEX_TIP) || !visible(MIDDLE_TIP)) return false;
        float scale = handScale();
        float margin = scale * 0.25f;
        float minX = Math.min(wx(INDEX_TIP), wx(MIDDLE_TIP));
        float maxX = Math.max(wx(INDEX_TIP), wx(MIDDLE_TIP));
        return wx(THUMB_TIP) > minX - margin && wx(THUMB_TIP) < maxX + margin;
    }

    private float spreadAngle(int mcpA, int tipA, int mcpB, int tipB) {
        return boneAngle(mcpA, tipA, mcpB, tipB);
    }

    private boolean pointsSideways(int mcp, int tip) {
        if (!visible(mcp) || !visible(tip) || !visible(WRIST) || !visible(MIDDLE_MCP)) return false;
        float ang = boneAngle(mcp, tip, WRIST, MIDDLE_MCP);
        return ang > 60f;
    }

    private boolean crossed(int mcpA, int tipA, int mcpB, int tipB) {
        if (!visible(mcpA) || !visible(tipA) || !visible(mcpB) || !visible(tipB)) return false;
        boolean mcpOrder = wx(mcpA) < wx(mcpB);
        boolean tipOrder = wx(tipA) < wx(tipB);
        return mcpOrder != tipOrder;
    }

    private boolean indexUp()  { return extended(INDEX_MCP,  INDEX_PIP,  INDEX_DIP,  INDEX_TIP);  }
    private boolean middleUp() { return extended(MIDDLE_MCP, MIDDLE_PIP, MIDDLE_DIP, MIDDLE_TIP); }
    private boolean ringUp()   { return extended(RING_MCP,   RING_PIP,   RING_DIP,   RING_TIP);   }
    private boolean pinkyUp()  { return extended(PINKY_MCP,  PINKY_PIP,  PINKY_DIP,  PINKY_TIP);  }

    private boolean indexCurled()  { return curled(INDEX_MCP,  INDEX_PIP,  INDEX_DIP,  INDEX_TIP);  }
    private boolean middleCurled() { return curled(MIDDLE_MCP, MIDDLE_PIP, MIDDLE_DIP, MIDDLE_TIP); }
    private boolean ringCurled()   { return curled(RING_MCP,   RING_PIP,   RING_DIP,   RING_TIP);   }
    private boolean pinkyCurled()  { return curled(PINKY_MCP,  PINKY_PIP,  PINKY_DIP,  PINKY_TIP);  }

    public char detect(float[][] normalized) {
        return '?';
    }

    public char detect(HandData data) {
        if (data == null || !data.isPresent()) return '?';

        this.w   = data.world();
        this.img = data.landmarks();
        this.vis = data.visibility();

        if (!visible(WRIST)) return '?';

        boolean iUp   = indexUp();
        boolean mUp   = middleUp();
        boolean rUp   = ringUp();
        boolean pUp   = pinkyUp();
        boolean iCurl = indexCurled();
        boolean mCurl = middleCurled();
        boolean rCurl = ringCurled();
        boolean pCurl = pinkyCurled();
        boolean thumb = thumbAbducted();
        boolean allCurled = iCurl && mCurl && rCurl && pCurl;

        if (pUp && !iUp && !mUp && !rUp && !thumb) return 'I';

        if (pUp && thumb && !iUp && !mUp && !rUp) return 'Y';

        if (iUp && thumb && !mUp && !rUp && !pUp && !thumbTouches(INDEX_TIP)) return 'L';

        if (hooked(INDEX_MCP, INDEX_PIP, INDEX_DIP, INDEX_TIP)
                && mCurl && rCurl && pCurl) return 'X';

        if (iUp && !mUp && !rUp && !pUp && thumbTouches(MIDDLE_TIP)) return 'D';

        if (pointsSideways(INDEX_MCP, INDEX_TIP) && !mUp && !rUp && !pUp && thumb) {
            float[] fingerDir = vec(INDEX_MCP, INDEX_TIP);
            float[] palmDir   = vec(WRIST, MIDDLE_MCP);
            float downAngle   = angleBetween(fingerDir, palmDir);
            return downAngle > 120f ? 'Q' : 'G';
        }

        if (pointsSideways(INDEX_MCP, INDEX_TIP)
                && pointsSideways(MIDDLE_MCP, MIDDLE_TIP)
                && !rUp && !pUp) return 'H';

        if (iUp && mUp && !rUp && !pUp && thumb) {
            float ang = spreadAngle(INDEX_MCP, INDEX_TIP, MIDDLE_MCP, MIDDLE_TIP);
            if (ang < 28f) {
                boolean down = wy(INDEX_TIP) > wy(INDEX_MCP);
                return down ? 'P' : 'K';
            }
        }

        if (iUp && mUp && !rUp && !pUp && !thumb
                && crossed(INDEX_MCP, INDEX_TIP, MIDDLE_MCP, MIDDLE_TIP)) return 'R';

        if (iUp && mUp && !rUp && !pUp && !thumb) {
            float ang = spreadAngle(INDEX_MCP, INDEX_TIP, MIDDLE_MCP, MIDDLE_TIP);
            if (ang < 22f) return 'U';
            if (ang < 55f) return 'V';
        }

        if (iUp && mUp && rUp && !pUp) return 'W';

        if (iUp && mUp && rUp && pUp && !thumb) {
            float imAng = spreadAngle(INDEX_MCP,  INDEX_TIP,  MIDDLE_MCP, MIDDLE_TIP);
            float mrAng = spreadAngle(MIDDLE_MCP, MIDDLE_TIP, RING_MCP,   RING_TIP);
            if (imAng < 14f && mrAng < 14f) return 'B';
        }

        if (thumbTouches(INDEX_TIP) && !iUp && mUp && rUp && pUp) return 'F';

        if (hooked(INDEX_MCP, INDEX_PIP, INDEX_DIP, INDEX_TIP)
                && hooked(MIDDLE_MCP, MIDDLE_PIP, MIDDLE_DIP, MIDDLE_TIP)
                && hooked(RING_MCP, RING_PIP, RING_DIP, RING_TIP)
                && hooked(PINKY_MCP, PINKY_PIP, PINKY_DIP, PINKY_TIP)
                && thumbTucked()) return 'E';

        if (allCurled && thumbBetweenIndexMiddle()
                && jointAngle(THUMB_MCP, THUMB_IP, THUMB_TIP) < 135f) return 'T';

        if (allCurled && !thumb) {
            boolean threeOver = visible(THUMB_TIP)
                    && wy(INDEX_TIP)  > wy(THUMB_TIP) - 0.005f
                    && wy(MIDDLE_TIP) > wy(THUMB_TIP) - 0.005f
                    && wy(RING_TIP)   > wy(THUMB_TIP) - 0.005f;
            if (threeOver) return 'M';

            boolean twoOver = visible(THUMB_TIP)
                    && wy(INDEX_TIP)  > wy(THUMB_TIP) - 0.005f
                    && wy(MIDDLE_TIP) > wy(THUMB_TIP) - 0.005f
                    && wy(RING_TIP)   < wy(THUMB_TIP) + 0.010f;
            if (twoOver) return 'N';
        }

        if (allCurled && thumb && !thumbTucked()) return 'A';
        if (allCurled && thumb && thumbTucked() && !thumbBetweenIndexMiddle()) return 'S';

        boolean allHalf = halfCurled(INDEX_MCP, INDEX_PIP, INDEX_DIP, INDEX_TIP)
                && halfCurled(MIDDLE_MCP, MIDDLE_PIP, MIDDLE_DIP, MIDDLE_TIP)
                && halfCurled(RING_MCP,   RING_PIP,   RING_DIP,   RING_TIP)
                && halfCurled(PINKY_MCP,  PINKY_PIP,  PINKY_DIP,  PINKY_TIP);

        if (allHalf && thumbTouches(INDEX_TIP)) return 'O';
        if (allHalf && !thumbTouches(INDEX_TIP)) return 'C';

        return '?';
    }

    public static char classify(float[][] normalized) {
        return new FallbackDetector().detect(normalized);
    }

    public static char classify(HandData data) {
        return new FallbackDetector().detect(data);
    }

    public DebugSnapshot debugDetect(HandData data) {
        if (data == null || !data.isPresent()) return DebugSnapshot.empty();

        this.w   = data.world();
        this.img = data.landmarks();
        this.vis = data.visibility();

        if (!visible(WRIST)) return DebugSnapshot.empty();

        float[] joints = {
                jointAngle(THUMB_CMC,  THUMB_MCP,  THUMB_IP),
                jointAngle(THUMB_MCP,  THUMB_IP,   THUMB_TIP),
                jointAngle(INDEX_MCP,  INDEX_PIP,  INDEX_DIP),
                jointAngle(INDEX_PIP,  INDEX_DIP,  INDEX_TIP),
                jointAngle(MIDDLE_MCP, MIDDLE_PIP, MIDDLE_DIP),
                jointAngle(MIDDLE_PIP, MIDDLE_DIP, MIDDLE_TIP),
                jointAngle(RING_MCP,   RING_PIP,   RING_DIP),
                jointAngle(RING_PIP,   RING_DIP,   RING_TIP),
                jointAngle(PINKY_MCP,  PINKY_PIP,  PINKY_DIP),
                jointAngle(PINKY_PIP,  PINKY_DIP,  PINKY_TIP)
        };

        float[] spreads = {
                spreadAngle(INDEX_MCP,  INDEX_TIP,  MIDDLE_MCP, MIDDLE_TIP),
                spreadAngle(MIDDLE_MCP, MIDDLE_TIP, RING_MCP,   RING_TIP),
                spreadAngle(RING_MCP,   RING_TIP,   PINKY_MCP,  PINKY_TIP)
        };

        boolean[] thumbState = {
                thumbAbducted(),
                thumbTucked(),
                thumbBetweenIndexMiddle(),
                thumbTouches(INDEX_TIP),
                thumbTouches(MIDDLE_TIP)
        };

        boolean[] fingerState = {
                indexUp(), middleUp(), ringUp(), pinkyUp(),
                indexCurled(), middleCurled(), ringCurled(), pinkyCurled()
        };

        char result = detect(data);
        return new DebugSnapshot(result, joints, spreads, thumbState, fingerState, handScale(), vis);
    }

    public record DebugSnapshot(
            char result,
            float[] joints,
            float[] spreads,
            boolean[] thumbState,
            boolean[] fingerState,
            float handScale,
            float[] visibility
    ) {
        public static DebugSnapshot empty() {
            return new DebugSnapshot('?', new float[10], new float[3], new boolean[5], new boolean[8], 0f, new float[21]);
        }

        @Override
        public String toString() {
            return String.format(
                    "[FallbackDetector] result='%s'  scale=%.4f%n" +
                            "  thumb  cmc=%.1f° ip=%.1f°  abducted=%b  tucked=%b  betweenIM=%b  touchesIndex=%b  touchesMiddle=%b%n" +
                            "  index  pip=%.1f°  dip=%.1f°  up=%b  curled=%b%n" +
                            "  middle pip=%.1f°  dip=%.1f°  up=%b  curled=%b%n" +
                            "  ring   pip=%.1f°  dip=%.1f°  up=%b  curled=%b%n" +
                            "  pinky  pip=%.1f°  dip=%.1f°  up=%b  curled=%b%n" +
                            "  spread  IM=%.1f°  MR=%.1f°  RP=%.1f°%n" +
                            "  visibility  wrist=%.2f  index=%.2f  middle=%.2f  ring=%.2f  pinky=%.2f",
                    result, handScale,
                    joints[0], joints[1], thumbState[0], thumbState[1], thumbState[2], thumbState[3], thumbState[4],
                    joints[2], joints[3], fingerState[0], fingerState[4],
                    joints[4], joints[5], fingerState[1], fingerState[5],
                    joints[6], joints[7], fingerState[2], fingerState[6],
                    joints[8], joints[9], fingerState[3], fingerState[7],
                    spreads[0], spreads[1], spreads[2],
                    visibility[0], visibility[8], visibility[12], visibility[16], visibility[20]
            );
        }
    }
}