package frc.robot.subsystems;

import java.nio.channels.ServerSocketChannel;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj.util.Color;

public class LEDSubsystem extends SubsystemBase {
    private AddressableLED led;
    private AddressableLEDBuffer ledBuffer;

    int[] RGBWData;
    int ledCount;

    public String execute = "";

    private int tick = 0;

    public LEDSubsystem(int ledCount) {
        led = new AddressableLED(8);
            this.ledCount = ledCount;
        RGBWData = new int[ledCount * 4];
        ledBuffer = new AddressableLEDBuffer(ledCount+(int) Math.floor(ledCount/3));
        led.setLength(ledBuffer.getLength());
        led.setData(ledBuffer);
        led.start();
    }
    @Override
    public void periodic() {
        //for (int i=0;i<ledCount-1;i++) {
        //    setDataRGBW(i,0,0,(int) (125*(Math.sin(i)+1)/2),0);
        //}
        if (DriverStation.isTeleopEnabled()) {
            execute = "custom";
        } else if (DriverStation.isAutonomousEnabled()) {
            execute = "sine";
        } else if (DriverStation.isTest()) {
            execute = "none";
        }

        tick+=1;
        switch (execute) {
            case "rainbow":
                rainbow(tick,0.8,1,0); break;
            case "sine":
                sineColors(new Color[] {
                    new Color(10,255,0),
                    new Color(0, 10, 255)
                   ,new Color(255,0,10)
                    }, tick, 1, 1); break;
            case "custom":
                customPattern(new double[] {
                    1,1,1,1,1,0,0,0,0,0
                }, new Color[] {
                    Color.kBlue, Color.kCoral, Color.kWhite
                }, "Sine", 1, tick, 0.1,1);
            break;
            default: setAllDataRGBW(0, 0, 0, 0);break;
        }
        pushData();
        led.setData(ledBuffer);
    }

    /* Shaders */
    Color getMixedColor(Color color1, Color color2, double ratio) {
        double r = (color1.red * (1 - ratio) + (color2.red * ratio));
        double g = (color1.green * (1 - ratio) + (color2.green * ratio));
        double b = (color1.blue * (1 - ratio) + (color2.blue * ratio));
        return new Color(r,g,b);
    }
    Color getShaderedColor(double ci, Color[] colors, double ratio, double colorLen) {
        int colorCount = colors.length;
        int colorIndex = (int) Math.floor((ci % (colorLen * colorCount))/colorLen);

        Color startColor = colors[colorIndex];
        Color nextColor = colors[(colorIndex + 1) % colorCount];

        double r = (startColor.red * (1 - ratio) + (nextColor.red * ratio));
        double g = (startColor.green * (1 - ratio) + (nextColor.green * ratio));
        double b = (startColor.blue * (1 - ratio) + (nextColor.blue * ratio));
        return new Color(r,g,b);
    }

    /* Periodic Funcitons */
    public void rainbow(int tick, double speed, double change, int w) {
        double rainbow = tick*speed;
        for (int i=0;i<ledCount-1;i++) {
            Color c = Color.fromHSV((int) (rainbow+(i*change) % 180), 255, 255);
            setDataRGBW(i, (int) (c.red*255), (int) (c.green*255), (int) (c.blue*255), w);
        }
    }
    void sineColors(Color[] colors, int tick, double speed, int mult) {
        for (int i=0; i < ledCount; i++) {
            double colorLen = ledCount / (mult + 1); // Length of One Color (in LEDs)
            double ratio = (1-Math.cos(Math.PI * ((i+(tick*speed)) % colorLen)/colorLen))/2; // Cosine Ratio
            //if ((tick%60)==0 && (i%20)==0) {
                //out("Index:"+i+" ratio:"+ratio);
                //out("Index:"+i+" cIndex:"+colorIndex+" c:"+r+" "+g+" "+b);
            //}
            setDataRGBW(i, getShaderedColor(i+(tick*speed), colors, ratio, colorLen), 0);
        }
    }
    void linColors(Color[] colors, int tick, double speed, int mult) {
        for (int i=0; i < ledCount; i++) {
            double colorLength = ledCount / (mult+1);
            double ratio = ((i+(tick*speed)) % colorLength)/colorLength;

            setDataRGBW(i,getShaderedColor(i+(tick*speed), colors, ratio, colorLength),0);
        }
    }
    /**
     * A custom LED pattern with custom colors!
     * @param pattern Pattern to repeat (0~1) in brightness
     * @param colors Colors to use
     * @param shaderType Color Shader to use: Solid Linear Sine
     */
    void customPattern(double[] pattern, Color[] colors, String shaderType, int shaderSetting, int tick, double speed, int mult) {
        int patternLen = pattern.length;
        for (int i=0;i<ledCount;i++) {
            double colorMult = pattern[i%patternLen];
            Color outCol;
            if (colors.length==1) {
                outCol = colors[0];
                if (shaderType == "Sine") {
                    int colorLength=ledCount/(mult+1);
                    colorMult*=(1-Math.cos(Math.PI*((i+(tick*speed)%colorLength))/colorLength))/2;
                }
            } else {
                double colorLen = ledCount/(mult+1);
                double ratio = 0;
                switch (shaderType) {
                    case "Linear":
                        ratio = ((i+(tick*speed)) % colorLen)/(colorLen*shaderSetting); break;
                    case "Sine":
                        ratio = (1-Math.cos(Math.PI * ((i+(tick*speed)) % colorLen)/colorLen))/(2*shaderSetting); break;
                    case "Solid": break; default: break;
                }
                outCol = getShaderedColor(i+(tick*speed), colors, ratio, colorLen);
            }
            outCol = new Color(outCol.red*colorMult,outCol.green*colorMult,outCol.blue*colorMult);
            setDataRGBW(i,outCol,0);
        }
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
    void sinColor(int r, int g, int b, double waves, double center, double amp, double tscroll) {
        double lambda = ledBuffer.getLength() / (2 * waves);
        for (int i = 0; i < 20; ++i) {
            double fx = Math.cos((i / lambda) * Math.PI + tick*0.01 * tscroll * Math.PI) * amp + center;
            setDataRGBW(i,
                    MathUtil.clamp((int) (r * fx), 0, 255),
                    MathUtil.clamp((int) (g * fx), 0, 255),
                    MathUtil.clamp((int) (b * fx), 0, 255),0);
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
    public void setDataRGBW(int index, Color color, int W) {
        RGBWData[(index * 4)] = (int) (color.red*255);
        RGBWData[(index * 4) + 1] = (int) (color.green*255);
        RGBWData[(index * 4) + 2] = (int) (color.blue*255);
        RGBWData[(index * 4) + 3] = W;
    }
    public void setRGBW(int index, int R, int G, int B, int W) {
        int trueIndex = index + (int) Math.floor(index/3); // Index offset ONLY FOR ENCODING INTO GRB
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
                setRGB(trueIndex, R, G, B); // R G B
                setRGB(trueIndex+1, nextRGBW[1], W, nextRGBW[0]); // +g W +r
                break;
            case 1:
                setRGB(trueIndex, G, prevRGBW[3], R); // G -w R
                setRGB(trueIndex+1, W, B, nextRGBW[1]); // W B +g FOR B, REMEMBER THAT THE INDEX is 1
                break;
            case 2:
                setRGB(trueIndex, prevRGBW[3], prevRGBW[2], G); // -w -b G
                setRGB(trueIndex+1, B, R, W); // B R W
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
    void setAllDataRGBW(int r, int b, int g, int w) {
        for (int i=0;i<ledCount;i++) {
            setDataRGBW(i,r,g,b,w);
        }
    }
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
