package frc.robot.subsystems;

import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj.util.Color;

public class LEDSubsystem extends SubsystemBase {
    private AddressableLED led;
    private AddressableLEDBuffer ledBuffer;

    int[] RGBWData;
    int ledCount;

    private int tick = 0;

    public LEDSubsystem() {
        led = new AddressableLED(8);
        ledCount=40;
        RGBWData = new int[ledCount * 4];
        ledBuffer = new AddressableLEDBuffer(ledCount);
        led.setLength(ledBuffer.getLength());
        led.setData(ledBuffer);
        led.start();
    }
    @Override
    public void periodic() {
        rainbow(tick, 1, 10, 0);

        tick+=1;

        push();
        led.setData(ledBuffer);
    }

    /* Periodic Funcitons */
    public void rainbow(int tick, int speed, int change, int w) {
        int rainbow = (tick*speed) % 180;
        for (int i=0;i<ledCount-1;i++) {
            Color c = Color.fromHSV(rainbow-(i*change), 255, 255);
            setDataRGBW(i, (int) c.red, (int) c.green, (int) c.blue, w);
        }
        //setAllDataRGBW(rc,0);
    }

    /* Old Periodic */
    void rainbow1(int tick) {
        for (int i = 0; i < ledBuffer.getLength(); ++i) {
            int hue = (tick + i * 180 / ledBuffer.getLength()) % 180;
            ledBuffer.setHSV(i, hue, 255, 128);
        }
    }
    public void rainbow2(int tick, double difference) {
        tick*=0.1;
        for (int i=0;i<ledCount-1;i++) {
            double j = i*(Math.PI-difference);
            setDataRGBW(i,
            (int) (Math.sin(tick+j)*255),
            (int) (Math.sin(tick+2+j)*255),
            (int) (Math.sin(tick+4+j)*255),0);}
    }
    boolean toFlash(int tick, int period) {
        int flash = tick % period;
        return flash < period/2;
    }
    boolean toFlash(double hz) {
        double period = 1.0 / hz;
        double v = ((Timer.getFPGATimestamp()) % period) / period;
        return v < 0.5;
    }

    void sineColor(int r, int g, int b, double waves, double center, double amp, double tscroll) {
        double lambda = ledBuffer.getLength() / (2 * waves);
        for (int i = 0; i < 20; ++i) {
            double fx = Math.cos((i / lambda) * Math.PI + Timer.getFPGATimestamp() * tscroll * Math.PI) * amp + center;
            setRGB(i,
                    Math.min((int) (r * fx), 255),
                    Math.min((int) (g * fx), 255),
                    Math.min((int) (b * fx), 255));
        }
    }

    /* RGBW Compatibility */
    public int[] getDataRGBW(int index) {
        return new int[] {
            RGBWData[index * 4],
            RGBWData[(index * 4) + 1],
            RGBWData[(index * 4) + 2],
            RGBWData[(index * 4) + 3]};
    }
    public void setDataRGBW(int index, int R, int G, int B, int W) {
        RGBWData[(index * 4)] = R;
        RGBWData[(index * 4) + 1] = G;
        RGBWData[(index * 4) + 2] = B;
        RGBWData[(index * 4) + 3] = W;
    }
    public void setRGBW(int RGBWIndex, int R, int G, int B, int W) {
        int RGBIndex = RGBWIndex + (int) Math.floor(RGBWIndex/3); // Index offset ONLY FOR ENCODING INDO GRBGRBGRBGRB
        int[] prevRGBW = getDataRGBW(RGBWIndex);
        if (RGBWIndex>0) {prevRGBW = getDataRGBW(RGBWIndex-1);}

        int[] nextRGBW = getDataRGBW(RGBWIndex+1);

        //print(prevRGB.toString());
        /** HOW TO READ R G B comments
         * I will use C and V as example
         * C=C: Use C to output as C. basically just normal setRGB
         * C=V: Use V to output as C. for example G=W (W is the input value from us)
         * C=+V: Use the next index's V to output
         * C=-V: Use the previous index's V to output.
         * 
         * REMEMBER: you use the INDEX, not the OFFSETINDEX
         */

        switch (RGBWIndex % 3) {
            case 0:
                ledBuffer.setRGB(RGBIndex, R, G, B); // R=R G=G B=B
                ledBuffer.setRGB(RGBIndex+1, nextRGBW[1], W, nextRGBW[0]); // R=+G G=W B=+R
                break;
            case 1:
                ledBuffer.setRGB(RGBIndex, G, prevRGBW[3], R); // R=G G>-W B=R
                ledBuffer.setRGB(RGBIndex+1, W, B, nextRGBW[1]); // R=W G=B B=+G FOR B, REMEMBER THAT THE INDEX is 1
                break;
            case 2:
                ledBuffer.setRGB(RGBIndex, prevRGBW[3], prevRGBW[2], G); // R=-W G=-B B=G
                ledBuffer.setRGB(RGBIndex+1, B, R, W); // R=B G=R B=W
                break;
            default:break;
        }
    }

    /* Quick */
    void setAllSolid(int r, int g, int b) {
        for (int i = 0; i < ledBuffer.getLength(); ++i)
            setRGB(i, r, g, b);
    }
    void setAllSolid(Color color) {
        for (int i = 0; i < ledBuffer.getLength(); ++i)
            setColor(i, color);
    }

    public void push() { // This sets the RGBW
        for (int i=0; i<ledCount-1;i++) {
            setRGBW(i, RGBWData[i * 4], RGBWData[i * 4 + 1], RGBWData[i * 4 + 2], RGBWData[i * 4 + 3]);}
    }

    /* Shortened Names */
    public void setRGB(int n, int r, int g, int b) {
        ledBuffer.setRGB(n, r, g, b);}
    public void setColor(int n, Color color) {
        ledBuffer.setRGB(n, (int) (color.red * 255), (int) (color.green * 255), (int) (color.blue * 255));}
    public void setHSV(int n, int h, int s, int v) {
        ledBuffer.setHSV(n, h, s, v);}
    public void off() {setAllSolid(0, 0, 0);}
}
