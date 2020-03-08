package com.narc.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : Narcssus
 * @date : 2020/3/8 1:33
 */
public class FileUtils {

    public static List<String> readFileToListByLine(String filePath) {
        List<String> res = new ArrayList<String>();
        File filename = new File(filePath);
        InputStreamReader is = null;
        BufferedReader br = null;
        try {
            is = new InputStreamReader(new FileInputStream(filename));
            br = new BufferedReader(is);
            String line = br.readLine();
            while (line != null) {
                res.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

}
