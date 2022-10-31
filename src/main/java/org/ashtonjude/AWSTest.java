package org.ashtonjude;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileInputStream;

public class AWSTest {
    public static void main(String args[]) throws Exception  //static method
    {
        String bernie = IOUtils.toString(new FileInputStream("bernie.txt"), "UTF-8");
        String kanye = IOUtils.toString(new FileInputStream("kanye.txt"), "UTF-8");
//        String keanu = IOUtils.toString(new FileInputStream("keanu.txt"), "UTF-8");
//        String jimin = IOUtils.toString(new FileInputStream("jimin.txt"), "UTF-8");
//
//        //ProcessPhoto.compareFaces(image1, image1);
//        System.out.println("bernie Emotion:" + ProcessPhoto.getEmotion(bernie));
//        System.out.println("kanye Emotion:" + ProcessPhoto.getEmotion(kanye));
//        System.out.println("keanu Emotion:" + ProcessPhoto.getEmotion(keanu));
//        System.out.println("jimin Emotion:" + ProcessPhoto.getEmotion(jimin));
        //Student student = ProcessPhoto.processPhoto(kanye);

        //jsonTest();
    }
}
