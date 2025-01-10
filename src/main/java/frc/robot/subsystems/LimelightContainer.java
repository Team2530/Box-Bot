// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import java.util.ArrayList;

import com.studica.frc.AHRS;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.LimelightHelpers;
import frc.robot.LimelightHelpers.LimelightResults;
import frc.robot.LimelightHelpers.RawDetection;
import frc.robot.LimelightHelpers.RawFiducial;

public class LimelightContainer extends SubsystemBase {
  private static ArrayList<String> limelights = new ArrayList<String>();
  static boolean maximizeFPS = false;
  
  /** 
   * Creates a new Limelight with ID id.
   */
  public LimelightContainer(String[] ids, boolean maximizeFPS) {
    for (String id : ids) {
      limelights.add(id);
      System.out.println("Initilaized Limelight: " + id);
      LimelightContainer.maximizeFPS = maximizeFPS;
    }
  }

  @Override
  public void periodic() {
    if (maximizeFPS) {
      for (String limelight : limelights) {
        
          cropViewport(limelight);
        }
      }
    }


    

  private void cropViewport(String limelight) {

    LimelightResults results = LimelightHelpers.getLatestResults(limelight);

    if(results.valid) {
      System.out.println("Yessir");
      if(results.targets_Fiducials.length > 0) {
        System.out.println("YEP");
      }
    }

       // LimelightHelpers.setCropWindow();
    }

   
    


  public void estimateMT2Odometry(SwerveDrivePoseEstimator poseEstimator, ChassisSpeeds speeds, AHRS navx) {
    for (String limelight : limelights) {
      boolean doRejectUpdate = false;
      LimelightHelpers.SetRobotOrientation(limelight, poseEstimator.getEstimatedPosition().getRotation().getDegrees(), 0, 0, 0, 0, 0);
      LimelightHelpers.PoseEstimate mt2 = LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2(limelight);
      if(Math.abs(navx.getRate()) > 720)
      {
        doRejectUpdate = true;
      }
      if(mt2.tagCount == 0)
      {
        doRejectUpdate = true;
      }
      if(!doRejectUpdate)
      {
        poseEstimator.setVisionMeasurementStdDevs(VecBuilder.fill(.7,.7,9999999));
        poseEstimator.addVisionMeasurement(
            mt2.pose,
            mt2.timestampSeconds);
      }
    }
  }

}
