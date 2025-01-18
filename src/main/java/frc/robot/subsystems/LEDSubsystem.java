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
        ledCount = 80;
        RGBWData = new int[ledCount * 4];
        ledBuffer = new AddressableLEDBuffer(ledCount+(int) Math.floor(ledCount/3));
        led.setLength(ledBuffer.getLength());
        led.setData(ledBuffer);
        led.start();
    }
    @Override
    public void periodic() {
        //for (int i=0;i<ledCount-1;i++) {
        //    setDataRGBW(i,0,255,0,0);
        //}
        tick+=1;
        //System.out.println(tick+" r: ");
        rainbow(tick,0.8,1,0);
        //rainbow2(tick,0.01);
        
        pushData();
        led.setData(ledBuffer);
    }

    /* Periodic Funcitons */
    public void rainbow(int tick, double speed, double change, int w) {
        double rainbow = tick*speed;
        for (int i=0;i<ledCount-1;i++) {
            Color c = Color.fromHSV((int) (rainbow+(i*change) % 180), 255, 255);
            setDataRGBW(i, (int) (c.red*255), (int) (c.green*255), (int) (c.blue*255), w);
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
            double j = i*difference;
            setDataRGBW(i,
            (int) (Math.sin(tick+j)*255),
            (int) (Math.sin(tick+2*j)*255),
            (int) (Math.sin(tick+3*j)*255),0);}
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
            setDataRGBW(i,
                    Math.min((int) (r * fx), 255),
                    Math.min((int) (g * fx), 255),
                    Math.min((int) (b * fx), 255),0);
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
    public void setRGBW(int index, int R, int G, int B, int W) {
        int trueIndex = index + (int) Math.floor(index/3); // Index offset ONLY FOR ENCODING INTO GRBGRBGRBGRB
        int[] prevRGBW = getDataRGBW(index);
        if (index>0) {prevRGBW = getDataRGBW(index-1);}

        int[] nextRGBW = getDataRGBW(index);
        if (!(index+1>ledCount)) {nextRGBW=getDataRGBW(index+1);}

        /** HOW TO READ R G B comments
         * I will use C (Current) and V (Value) as example
         * C=C: Use C to output as C. basically just normal setRGB
         * C=V: Use the V to output as C. for example G=W (W is the input value from here)
         * C=+V: Use the next index's V to output
         * C=-V: Use the previous index's V to output.
         * Lowercase: Previous or After's
         * 
         * Cases:           case0 case1 case2 case0
         * YOUR input:      RGB W|RG BW|R GBW|RGBW|... 
         * case 0:          RGB g Wr          RGB    
         * case 1:              G wR WB g
         * case 2:                   wb G BRW    
         * input:           RGB|R GB|RG B|RGB|RGB|...
         *                    reorders correctly
         * setLED:          BGR|B GR|BG R|BGR|BGR|...
         *                    reorders correctly
         * WPILIB sends:    GRB|G RB|GR B|GRB|GRB|...
         * output:          GRB W|GR BW|G RBW|GRBW|...
         */

        switch (index % 3) {
            case 0:
                setRGB(trueIndex, R, G, B); // R=R G=G B=B
                setRGB(trueIndex+1, nextRGBW[1], W, nextRGBW[0]); // R<+G G<W B<+R
                break;
            case 1:
                setRGB(trueIndex, G, prevRGBW[3], R); // R<G G<-W B<R
                setRGB(trueIndex+1, W, B, nextRGBW[1]); // R=W G=B B=+G FOR B, REMEMBER THAT THE INDEX is 1
                break;
            case 2:
                setRGB(trueIndex, prevRGBW[3], prevRGBW[2], G); // R=-W G=-B B=G
                setRGB(trueIndex+1, B, R, W); // R=B G=R B=W
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

    public void pushData() { // This sets the RGBW
        for (int i=0; i<ledCount-1;i++) {
            setRGBW(i, RGBWData[i * 4], RGBWData[i * 4 + 1], RGBWData[i * 4 + 2], RGBWData[i * 4 + 3]);
        }
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
