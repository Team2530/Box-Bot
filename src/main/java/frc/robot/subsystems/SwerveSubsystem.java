/* 
package frc.robot.subsystems;
import org.photonvision.PhotonCamera;

public class SwerveSubsystem {
    private PhotonCamera coralLeft = new PhotonCamera("photonvision-coralLeft");

    public void updateMegaTagOdometry() {
        boolean doRejectUpdate = false;

        if (Math.abs(navX.getRate()) > 720) // if our angular velocity is greater than 720 degrees per second, ignore vision updates                                    // vision updates
        {
            doRejectUpdate = true;
        }

        if (!doRejectUpdate) {
            odometry.setVisionMeasurementStdDevs(VecBuilder.fill(2, 2, 9999999));

            odometry.addVisionMeasurement(
                    mt2.pose,
                        mt2.timestampSeconds);
        }
    }

}
*/