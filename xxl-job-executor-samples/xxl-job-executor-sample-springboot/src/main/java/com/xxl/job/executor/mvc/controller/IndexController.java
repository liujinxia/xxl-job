package com.xxl.job.executor.mvc.controller;

import com.xxl.job.executor.ResponseModel;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.io.*;

@Controller
@EnableAutoConfiguration
@Slf4j
public class IndexController {

    @RequestMapping("/getFileFromRemote")
    @ResponseBody
    ResponseModel index(@RequestParam String projectPath, @RequestParam String dateStr) {
        //String cmd = "cd /tmp/xdebug && cat trace.*| grep -v 'require_once' |  grep -v '" + projectPath + "/vendor'  |awk '{if($6 != \"\") {print $6}}'| grep -E '::|->'|grep -v 'closure'|sort | uniq  | sed  's/->/::/g' >/tmp/xdebug/php_result_" + dateStr + ".txt";
        try {
            String cmdContent = "cd /tmp/xdebug && cat trace.*| grep -v 'require_once' |  grep -v '" + projectPath + "/vendor'  |awk '{if($6 != \"\") {print $6}}'| grep -E '::|->'|grep -v 'closure'|sort | uniq  | sed  's/->/::/g'";
            BufferedWriter out = new BufferedWriter(new FileWriter("./parse.sh"));
            out.write(cmdContent);
            out.close();
            System.out.println(cmdContent);
            String cmd = "sh ./parse.sh";
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            StringBuffer sb = new StringBuffer();
            while ((line = buffer.readLine()) != null) {
                if (line == null) {
                    break;
                }
                sb.append(line);
                sb.append("\n");
            }
            System.out.println(sb.toString());
            return ResponseModel.builder().code(0).msg("解析成功").data(sb.toString()).build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseModel.builder().code(-1).msg("解析失败").data(e.getMessage()).build();
        }

    }


    @PostMapping(value = "/upload")
    @ResponseBody
    ResponseModel uploadFileBufferToLocal(@RequestParam("file") MultipartFile file, @RequestParam("filePath")  String filePath) {

        //将文件缓冲到本地
        boolean localFile = createLocalFile(filePath, file);
        if (!localFile) {
            log.error("Create local file failed!");
            return ResponseModel.builder().code(-2).msg("日志创建失败").data("").build();
        }
        log.info("Create local file successfully");

        return ResponseModel.builder().code(0).msg("传输成功").data(filePath).build();
    }

    /**
     * 通过上传的文件名，缓冲到本地，后面才能解压、验证
     *
     * @param filePath 临时缓冲到本地的目录
     * @param file
     */
    public boolean createLocalFile(String filePath, MultipartFile file) {
        File localFile = new File(filePath);
        //先创建目录
        localFile.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String path = filePath + "/" + originalFilename;

        log.info("createLocalFile path = {}", path);

        localFile = new File(path);
        FileOutputStream fos = null;
        InputStream in = null;
        try {

            if (localFile.exists()) {
                //如果文件存在删除文件
                boolean delete = localFile.delete();
                if (delete == false) {
                    log.error("Delete exist file \"{}\" failed!!!", path, new Exception("Delete exist file \"" + path + "\" failed!!!"));
                }
            }
            //创建文件
            if (!localFile.exists()) {
                //如果文件不存在，则创建新的文件
                localFile.createNewFile();
                log.info("Create file successfully,the file is {}", path);
            }

            //创建文件成功后，写入内容到文件里
            fos = new FileOutputStream(localFile);
            in = file.getInputStream();
            byte[] bytes = new byte[1024];

            int len = -1;

            while ((len = in.read(bytes)) != -1) {
                fos.write(bytes, 0, len);
            }

            fos.flush();
            log.info("Reading uploaded file and buffering to local successfully!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.error("InputStream or OutputStream close error : {}", e);
                return false;
            }
        }

        return true;
    }
}

