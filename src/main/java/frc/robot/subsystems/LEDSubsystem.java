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
        //rainbow(tick,0.8,1,0);
        sineColors(new Color[] {Color.kRed,Color.kBlue}, tick, 0.2, 5);
        
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
    boolean toFlash(int tick, int period) {
        int flash = tick % period;
        return flash < period/2;
    }
    boolean toFlash(double hz) {
        double period = 1.0 / hz;
        double v = ((Timer.getFPGATimestamp()) % period) / period;
        return v < 0.5;
    }
    
    void sineColors(Color[] colors, int tick, double speed, int mult) {
        int colorCount = colors.length;
        for (int i=0; i < ledCount; i++) {
            double colorLength = ledCount / (mult + 1); // Length of One Color (in LEDs)

            double ratio = (1-Math.cos(Math.PI * ((i+(tick*speed)) % colorLength)/colorLength))/2;
            int colorIndex = (int) Math.floor(((i+(tick*speed)) % (colorLength * colorCount))/colorLength);

            Color startColor = colors[colorIndex];
            Color endColor = colors[(colorIndex + 1) % colorCount];

            int r = (int)((startColor.red * (1 - ratio) + endColor.red * ratio)*255);
            int g = (int)((startColor.green * (1 - ratio) + endColor.green * ratio)*255);
            int b = (int)((startColor.blue * (1 - ratio) + endColor.blue * ratio)*255);
            //if ((tick%60)==0 && (i%20)==0) {
                //out("Index:"+i+" ratio:"+ratio);
                //out("Index:"+i+" cIndex:"+colorIndex+" c:"+r+" "+g+" "+b);
            //}
            setDataRGBW(i, r, g, b, 0);
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
         * 
         * Lowercase: Previous or After's
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
    public void setRGBW(int index, Color color, int W) {
        int R=(int)color.red*255; int G=(int)color.green*255; int B=(int)color.blue*255;
        int trueIndex = index + (int) Math.floor(index/3); // Index offset ONLY FOR ENCODING INTO GRBGRBGRBGRB
        int[] prevRGBW = getDataRGBW(index);
        if (index>0) {prevRGBW = getDataRGBW(index-1);}
        int[] nextRGBW = getDataRGBW(index);
        if (!(index+1>ledCount)) {nextRGBW=getDataRGBW(index+1);}
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
    void out(Object msg) {
        System.out.println(msg);
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
}
