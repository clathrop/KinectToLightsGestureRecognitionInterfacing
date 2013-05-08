/*
Body-Remotes Kinect Side Source Code
Carter Lathrop & Jacob Schwartz

This code represents the image processing, gesture tracking, and skeletal
recognition capabilities for our project. The setup() function is only
called once and sets up/initializes any data needed for the length of the
program. The draw() function is called multiple times a second and it is 
what gives us the processed image of ourselves through the infrared camera.
The draw function also is used to call the skeleton recognition function
which draws blue lines across all major limbs for each human body that is 
calibrated. More specific information about each function and its purpose
will be provided surrounding individual functions and areas of code below.

*/

//import music library for processing
import ddf.minim.*;
//java utilities
import java.util.*; 
//image processing lib from processing API
import processing.opengl.*;
//serial library to communicate with arduino
import processing.serial.Serial;
import SimpleOpenNI.SimpleOpenNI;
import java.io.*;
import java.lang.Object;

Minim minim;
AudioPlayer song;
SimpleOpenNI kinect;
Serial myPort;
//defines initializing and using serial communication
//set to false if no arduino is currently interfaced
boolean serial = true;
//keeps track of current user
int userID;
PVector userCenter = new PVector();

  long time1 = 0;
  long time2 = 0;
  long time3 = 0;
  long time4 = 0;
  long emptyRoomLightsTimeout = 0;
  long currentTime = 0;
  
  boolean stop = false;
  
   //the three gestures needed to unlock user access to lights control
  boolean pass1 = false;
  boolean pass2 = false;
  boolean pass3 = false;
  boolean passwordUnlock = false;
  int masterUser = 0;
  
  //stack used to keep track of users in space
  Stack st = new Stack();
  
  public class setLightsOff{
    
      //Timer object
  Timer timer = new Timer();

//timer function which turns light off if no users
  //in room for given amount of time
    public setLightsOff(int seconds) {
      //println("in setLightsOff");
      //set to turn lights off in 10 minutes
        timer.schedule(new lightsOff(), seconds*600000);
	}

    class lightsOff extends TimerTask {
        public void run() {
          if(stop){
            println("stop is on, dont turn off lights");
          }else{
            println("lights off");
            timer.cancel(); //Terminate the timer thread
        }
        }
    }
  }
  
  void stop()
{
  // the AudioPlayer you got from Minim.loadFile()
  song.close();
  minim.stop();
 
  // this calls the stop method that 
  // you are overriding by defining your own
  // it must be called so that your application 
  // can do all the cleanup it would normally do
  super.stop();
}

//called only once per program run, initializes variables
//such as the music object to play unlock user notification
//as well as sets the size of the window that displays the 
//camera feed
public void setup(){
  
  minim = new Minim(this);
  song = minim.loadFile("bloop.mp3");
   // instantiate a new context
  kinect = new SimpleOpenNI(this);
 
  // enable depthMap generation 
  kinect.enableDepth();
 
  // enable skeleton generation for all joints
  kinect.enableUser(SimpleOpenNI.SKEL_PROFILE_ALL);
 
  background(200,0,0);
  stroke(0,0,255);
  strokeWeight(3);
  smooth();
 
  // create a window the size of the depth information
  size(kinect.depthWidth(), kinect.depthHeight()); 
  
  
  if(serial){
    String portName = Serial.list()[0];
    myPort = new Serial(this, portName, 9600);
  }
}


//this function is called multiple times a second and updates the window
//with the camera feed with what is being seen through the camera. Also
//for every skeleton that is being tracked, it draws() the new location/
//updated position of that skeleton. Also for each skeleton, headTouch,
//our custom gesture recognition function is called to listen for any
//and all gestures that we designed.
public void draw(){
   // update the camera
  kinect.update();
 
  // draw depth image
  image(kinect.depthImage(),0,0); 
 
  // for all users from 1 to 10
  int i;
  for (i=1; i<=10; i++)
  {
    // check if the skeleton is being tracked
    if(kinect.isTrackingSkeleton(i))
    {
      drawSkeleton(i);  // draw the skeleton
      headTouch(i);
    }
  }
}
  
/*
Head touch first uses the SimpleOpenNI library to define the joint locations
for parts of the body on a user's skeleton. Once all necessary joints are 
defined are we able to find the proximity between joints. Because this function
is called multiple times a second and for each skeleton, it is very accurate
in letting us know when someone has completed gestures that we are looking for.
*/
private void headTouch(int userID){
  
  PVector head = new PVector();
  PVector neck = new PVector();
  PVector torso = new PVector();
  //right arm vectors
  PVector lHand = new PVector();
  PVector lElbow = new PVector();
  PVector lShoulder = new PVector();
  //left arm vectors
  PVector rHand = new PVector();
  PVector rElbow = new PVector();
  PVector rShoulder = new PVector();
  
  //head
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_HEAD, head);
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_NECK, neck);
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_TORSO, torso);
  //right arm
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_RIGHT_HAND, lHand);
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_RIGHT_ELBOW, lElbow);
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_RIGHT_SHOULDER, lShoulder);
  //left arm
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_LEFT_HAND, rHand);
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_LEFT_ELBOW, rElbow);
  kinect.getJointPositionSkeleton(userID, SimpleOpenNI.SKEL_LEFT_SHOULDER, rShoulder);
  
  PVector lForearm = PVector.sub(lShoulder, lElbow);
  PVector lArm = PVector.sub(lElbow, lHand);
  PVector rForearm = PVector.sub(rShoulder, rElbow);
  PVector rArm = PVector.sub(rElbow, rHand);
  //the line perpendicular to the head that connects both shoulder points
  PVector collar = PVector.sub(rShoulder, lShoulder);
  PVector neckLine = PVector.sub(head, neck);
  


//This starts the password gesture recognition wherein the first gesture
//that the system looks for is if the right hand touches the individual's
//torso if it does, flip pass1 to true. Only recognizes gesture from
//skeletons who are the masterUser or if no masterUser has been established
  if(torso.dist(rHand) < 200 && (masterUser == userID || masterUser == 0)){
    time1 = System.currentTimeMillis();
    //println("current time is: " + time1);
    pass1 = true;
  }
  
//second sequence of the password, left hand touches the torso
  if(torso.dist(lHand) < 200 && pass1 == true && (masterUser == userID || masterUser == 0)){
    time2 = System.currentTimeMillis();     
     if(time2 < (time1 + 10000)){
        pass2 = true;
   }
  }
   
//Third sequence, right hand must touch left elbow
   if(lElbow.dist(rHand) < 200 && pass2 == true && (masterUser == userID || masterUser == 0)){
    time3 = System.currentTimeMillis();     
     if(time3 < (time2 + 5000)){
        pass3 = true;
   }
   }

//Final sequence if left hand touches right elbow. Entire password sequence
//must have been completed within 5 seconds or it will not register and it 
//must be restarted from the beginning. The current user that completed the
//gesture, unlocks the use of the lights and their userId is now considered
//the masterUser ID
   if(rElbow.dist(lHand) < 200 && pass3 ==true && (masterUser == userID || masterUser == 0)){
    time4 = System.currentTimeMillis();
    if(time4 < (time1 + 5000)){
      
      song.play();
      song.rewind();

      //password becomes unlocked if was locked before and vice versa
      passwordUnlock = !passwordUnlock; 
      
      //if the password was unlocked, give masterUser to the userID 
      //that completed the password
      if(passwordUnlock){
        masterUser = userID;
      println("Unlocked by: " + userID);
      }
      //If the password was locked, do not give any one the masterUser ID
      //until someone else unlocks the password
      else if(!passwordUnlock){
        masterUser = 0;
        println("Locked by: " + userID);
      }
      
      pass1 = false;
      pass2 = false;
      pass3 = false;
      
    }else{
      println("password check timeout");
   }
   }
  

  //only allow lights control from the masterUser and ensure 
   //the password has been unlocked
  if(passwordUnlock && userID == masterUser){
    
  //-----BOTH HANDS TOUCHES HEAD -> BOTH LIGHTS TOGGLES-----
  if(head.dist(rHand) < 200 && head.dist(lHand) < 200){
    if(serial)

      //serial communication with arduino
      myPort.write('Z');
    println("headtouch " + userID);
  
  }else if(rHand.dist(lHand) > 600){ //---- HANDS MUST BE FAR APART TO NOT READ DOUBLE HEAD TAP-----
  
  //-----RIGHT HAND TOUCHES HEAD -> TOGGLES RIGHT LIGHT-----
    if(head.dist(rHand) < 200){
      if(serial)
        myPort.write('A');
      
      println("right hand head touch " + userID); 
      
    }
    //-----LEFT HAND TOUCHES HEAD -> LEFT LIGHT TOGGLE-----
    else if(head.dist(lHand) < 200){
      
      if(serial)
      myPort.write('B');
      
      println("left hand head touch " + userID);
      
      
    }
    //-----RIGHT HAND TOUCHES CHEST -> LEFT ARM CONTROLS DIM-----
    else if(torso.dist(rHand) < 200){
      myPort.write('1');
  
      println("right hand touching torso, LeftForearm vs collar angle is " + degrees(PVector.angleBetween(neckLine, lForearm)));
    
      float angleA = degrees(PVector.angleBetween(neckLine, lForearm));
      int angleAInt = (int) angleA;
      int scaledA = 255-(angleAInt)*255/180;
      
      if(serial && (scaledA != 48 && scaledA != 49 && scaledA != 50 && scaledA != 65 && scaledA != 66 && scaledA != 90))
        myPort.write(scaledA);    
        println("dim level is " + scaledA);
        
    }
       //-----Left HAND TOUCHES CHEST -> RIGHT ARM CONTROLS DIM-----
    else if ( torso.dist(lHand) < 200){
      
      myPort.write('2');
      println("left hand touching head, RightForearm vs collar angle is " + degrees(PVector.angleBetween(neckLine, rForearm)));
    
      float angleB = degrees(PVector.angleBetween(neckLine, rForearm));
      int angleBInt = (int) angleB;
      int scaledB = 255-(angleBInt)*255/180;
      
      if(serial  && (scaledB != 48 && scaledB != 49 && scaledB != 50 && scaledB != 65 && scaledB != 66 && scaledB != 90))
      myPort.write(scaledB);
      
      println("dim level is " + scaledB);

}else {
      if(serial)
      myPort.write('0');
      
    }
  }
}
   }

void drawPointCloud(int steps){
  //println("in drawPointCloud");
  int[] depthMap = kinect.depthMap();
  int index;
  PVector realWorldPoint;
  stroke(255);
  for(int y = 0; y < kinect.depthHeight(); y += steps){
    for(int x = 0; x < kinect.depthWidth(); x +=steps){
      index = x+y * kinect.depthWidth();
      if(depthMap[index]>0){
        realWorldPoint = kinect.depthMapRealWorld()[index];
        point(realWorldPoint.x, realWorldPoint.y, realWorldPoint.z);
      }
    }
  }
}

void drawUserPoints(int steps){
  //println("in drawUserPoints");
  int[] userMap = kinect.getUsersPixels(SimpleOpenNI.USERS_ALL);
  //draw the 3d point depth map
  PVector[] realWorldPoint = new PVector[kinect.depthHeight() * kinect.depthWidth()];
  int index;
  pushStyle();
  stroke(255);
  for(int y = 0; y < kinect.depthHeight(); y += steps){
    for(int x = 0; x < kinect.depthWidth(); x += steps){
      index = x + y * kinect.depthWidth();
      realWorldPoint[index] = kinect.depthMapRealWorld()[index].get();
      if(userMap[index] != 0){
        strokeWeight(2);
        stroke(0, 255, 0);
        point(realWorldPoint[index].x, realWorldPoint[index].y, realWorldPoint[index].z);
      }
    }
  }
  popStyle();
}


//called from draw multiple times a second and updates the new location
//of the user's skeletons
void drawSkeleton(int userID)
{  
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);
 
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);
 
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);
 
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
 
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);
 
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
  kinect.drawLimb(userID, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);  
}


void drawLimb(int userID, int jointType1, int jointType2){
  PVector jointPos1 = new PVector();
  PVector jointPos2 = new PVector();
  float confidence;
  
  //draw the joint position
  confidence = kinect.getJointPositionSkeleton(userID, jointType1, jointPos1);
  confidence = kinect.getJointPositionSkeleton(userID, jointType2, jointPos2);
  
  stroke(255, 0, 0, confidence * 200 + 55);
  line(jointPos1.x, jointPos1.y, jointPos1.z, jointPos2.x, jointPos2.y, jointPos2.z);
}

//function called when a new user is ID'd. Gives them a unique userID.
//No two users on screen can have the same userID. Used to differentiate
//users
public void onNewUser(int userID){
  println("onNewUser - userID: " + userID);
  //println(" start pose detection");
  kinect.startPoseDetection("Psi", userID);
  userID = userID;
  
  st.push(userID);
  println("stack: " + st);
  stop = true;
  
  
}

//function called when a user has been lost, usually when moved off screen
public void onLostUser(int userID){
  if(userID == masterUser){
    passwordUnlock = false;
    println("password has reset");
  }
  println("onLostUser - userID: " + userID);
  st.pop();
  println("stack after pop: " + st);
   
   println("stack empty?" + st.empty());
  if(st.empty()){
    stop = false;
    println("trying to run lightsoff");
   new setLightsOff(60);
    
  }

}

public void lightsOff(){
 println("lights have been turned off"); 
}

public void onStartCalibration(int userID){
  println("onStartCalibration - userID: " + userID);
}

public void onEndCalibration(int userID, boolean successfull){
  println("onEndCalibration - userID: " + userID + ", successful: " + successfull);
  if(successfull){
    println(" User Calibrated!!!!");
    kinect.startTrackingSkeleton(userID);
  }
  else{
    println(" Failed to calibrate user !!!");
    println(" Start pose detection");
    kinect.startPoseDetection("Psi", userID);
  }
}

public void onStartPose(String pose, int userID){
  //println("onStartPose - userID: " + userID + ", pose: " + pose);
  //println( "stop pose detection");
  
  kinect.stopPoseDetection(userID); 
 
  // start attempting to calibrate the skeleton
  kinect.requestCalibrationSkeleton(userID, true);
  
}

public void onEndPose(String pose, int userID){
  println("onEndPose - userID: " + userID + ", pose: " + pose);
}