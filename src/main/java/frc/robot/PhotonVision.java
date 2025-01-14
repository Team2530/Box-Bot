package frc.robot;

import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;

import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;

import com.studica.frc.AHRS;

import org.photonvision.PhotonCamera;

import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

import org.photonvision.PhotonUtils;

public class PhotonVision {

    public boolean validateTarget(PhotonTrackedTarget target) {
        if (target.getPoseAmbiguity() > 0.2)
            return false; // if the target is quite ambiguous, reject that type type
        if (target.getDetectedObjectConfidence() < 0)
            return false; // this value ranges from 0 to 1

        return true;
    };

    /**
     * This code does very coding code code things and stuff and
     */
    public Transform3d getCameraToRobotPose(String name) {
        // placeholder placeholder
        return new Transform3d();
    }

    public Pose3d getVisionPoseEstimate(PhotonCamera[] cameras, AHRS navX) {
        Pose3d weightedPose = new Pose3d(0, 0, 0, new Rotation3d());
        double totalWeight = 0;
        DoubleFunction<Double> weightFunction = (value) -> Math.pow(1 - value, 3);
        AprilTagFieldLayout aprilTagLayout = AprilTagFieldLayout.loadField(AprilTagFields.k2024Crescendo);

        for (PhotonCamera camera : cameras) {
            PhotonPipelineResult result = camera.getLatestResult(); // get all of the results for each camera, and put
                                                                    // them in a list
            List<PhotonTrackedTarget> targets = result.getTargets();
            for (PhotonTrackedTarget target : targets) {
                boolean acceptTarget = validateTarget(target);

                if (navX.getRate() > 720) { // if angular velocity is greater than 720 d / s
                    acceptTarget = false;
                }

                if (acceptTarget) {
                    // if it's good, account for camera position and add it to our estimation
                    Pose3d weightedTarget = PhotonUtils.estimateFieldToRobotAprilTag(
                        target.getBestCameraToTarget(),
                        aprilTagLayout.getTagPose(target.getFiducialId()).get(),
                        getCameraToRobotPose(camera.getName())
                    ).times(weightFunction.apply(target.getPoseAmbiguity())); // weight by ambiguity

                    weightedPose.plus(
                        new Transform3d(
                            weightedTarget.getTranslation(),
                            weightedTarget.getRotation()
                        )
                    );
                     
                    totalWeight += weightFunction.apply(target.getPoseAmbiguity());
                }
            }
        }

        return weightedPose.div(totalWeight);
    }
}
