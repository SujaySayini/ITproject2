
import java.io.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.lang.*;
import java.text.ParseException;
import java.nio.charset.StandardCharsets;

public class serverThread extends Thread {
    private Socket client;
    String FROM, USERAGENT, CONTENTTYPE, CONTENTLENGTH, PARAMETERS = "";
    //byte[] cgi_out;
    BufferedReader inFromClient;
    DataOutputStream outToClient;
    String lastSeen;

    public serverThread(Socket client) {
        this.client = client;

        try {
            client.setSoTimeout(5000);
        } catch (SocketException s) {
            sendErrorCode(408);
        }

        // Define global variable -> going to be used for communication w/ client later.
        try {
            inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outToClient = new DataOutputStream(client.getOutputStream());
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
            sendErrorCode(500);
        }

    }

    public void run() {
        String request;
        String[] requestParts;

        // if cant readline, 500
        try {
            this.client.setSoTimeout(5000); // wait for 5 sec
            request = inFromClient.readLine();
        } catch (SocketTimeoutException e) {
            try {
                byte[] byteMessage = "HTTP/1.0 408 Request Timeout".getBytes();
                outToClient.write(byteMessage);
                close();
                return;
            } catch (IOException io) {
                sendErrorCode(500);
                return;
            }
        } catch (IOException io) {
            sendErrorCode(500);
            return;
        }

        if (request == null) {
            sendErrorCode(400);
            return;
        }

        StringBuffer strbuff = new StringBuffer("");
        String line = null;

        try {
            while ((line = inFromClient.readLine()) != null) {
                String d;
                if (line.equals("")) {
                    d = "";
                } else {
                    d = "\r\n";
                }
                strbuff.append(line + d);
            }
        } catch (IOException e) {
            System.out.println("Error");
        }
        String[] Content = strbuff.toString().split("\r\n");

        // Extract each part of the request
        requestParts = request.split(" ");

        // If the request does not contain all relevant parts, 400 Bad Request
        if (requestParts.length != 3) {
            sendErrorCode(400);
            return;
        }

        // each part of the request:
        String method = requestParts[0];
        String file = requestParts[1];
        //String modified = "";

        if (file.equals("/"))
            file = "./index.html";
        else
            file = "." + file;

    // will find cookie header
    // put post = stringBuffer.toString().split("\r\n"); in main
    // put String cookie = "";
    // get file from file = requestParts[1];

        String cookie_value = "";
        //String[] Content = stringBuffer.toString().split("\r\n");
        if (Content.length == 4) {
            if (Content[3].contains("Cookie")) {
                // value of Cookie in header:
                cookie_value = Content[3].split(":")[1].trim();

                // Validates Cookie -> if valid -> change value in index_seen.html -> set file
                // to index_seen.html
                if (isValidCookie(cookie_value)) {
                    setLastSeen(cookie_value.split("=")[1]);
                    file = "./index_seen.html";
                }
            }
        }

        if (!isValidMethod(requestParts[0]))
            return;

        // if(method.equals("POST")){ //get our information into global variables
        // StringBuffer strbuff = new StringBuffer("");
        // String line;

        // try{
        // while ((line = inFromClient.readLine()) != null) {
        // String d;
        // if(line.equals("")){
        // d = "";
        // }else{
        // d = "\r\n";
        // }
        // strbuff.append(line + d);
        // }
        // }
        // catch(IOException e){
        // System.out.println("Error");
        // }
        // String []Content = strbuff.toString().split("\r\n");

        // FROM = Content[0].split(":")[1].trim();
        // USERAGENT = Content[1].split(":")[1].trim();

        // if(!Content[2].contains("Content-Type")){
        // sendErrorCode(500);
        // return;
        // }
        // CONTENTTYPE = Content[2].split(":")[1].trim();

        // if(!Content[3].contains("Content-Length")){
        // sendErrorCode(411);
        // return;
        // }
        // CONTENTLENGTH = Content[3].split(":")[1].trim();

        // if(!(Content.length < 5)){
        // //decode out parameters
        // String param = Content[4];
        // String decodedParams = "";

        // for(int i = 0;i<Content[4].length();i++){
        // if(param.charAt(i) == '!'){
        // continue;
        // }else{
        // decodedParams += param.charAt(i);
        // }
        // }
        // PARAMETERS = decodedParams.trim();
        // }
        // }

        // verify the version: if not 1.0, 505
        // String version = requestParts[2].split("/")[1];
        // if(!version.equals("1.0")){
        // sendErrorCode(505);
        // return;
        // }

        // try {
        //     if (inFromClient.ready())
        //         modified = inFromClient.readLine();
        // } catch (IOException e) {
        //     sendErrorCode(500);
        //     return;
        // }
        // boolean is304 = false;
        // Date modifiedDate = null;

        // // This segment is used to compare the IF-Modified-By field in the request.
        // if (!modified.trim().equals("")) {
        //     modified = modified.substring(modified.indexOf(":") + 2);
        //     try {
        //         modifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'").parse(modified);
        //         long requestTime = modifiedDate.getTime();

        //         File new_file = new File("." + file);
        //         long fileTime = new_file.lastModified();

        //         // convert times to long and compare.
        //         if (requestTime > fileTime && !method.equals("HEAD"))
        //             is304 = true;
        //     } catch (Exception e) {
        //         is304 = false;
        //     }
        // }
        // if (is304)
        //     sendErrorCode(304);

        // if (method.equals("POST")) {
        //     String e = getExtension(file);
        //     if (!(e.equals("cgi"))) {
        //         sendErrorCode(405);
        //         return;
        //     }
        // }

        // LAST CHECK
        System.out.println(file);
        if (isValidFile(file))
            handleRequest(method, file, 200);
    }
    public boolean isValidCookie(String CookieVal) {

        boolean returnVal = false;
        try {
            String decoded = URLDecoder.decode(CookieVal, "UTF-8");
            if (decoded.split("=")[0].equals("lasttime")){
                if (isValidDateFormat(decoded.split("=")[1]))
                        returnVal = true;
                else
                    returnVal = false;
            }
            else 
                returnVal = false;


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            returnVal = false;
        }

        return returnVal;

    }
    public boolean isValidDateFormat(String value) {
        //System.out.println(value);
        Date date = null;
        Date now = new Date();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date = sdf.parse(value);
            System.out.println(sdf.format(date));
            if (!value.equals(sdf.format(date))) {
                date = null;
            }
            if(!sdf.parse(sdf.format(now)).after(sdf.parse(value)))
                date = null;
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return date != null;
    }
    public void setLastSeen(String value) {
        try {
            String decoded = URLDecoder.decode(value, "UTF-8");
            String fullYear = decoded.split(" ")[0];           
            String fullTime = decoded.split(" ")[1];
           
            lastSeen = fullYear + " " + fullTime;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public String getExtension(String file) {
        String temp = file;
        return temp.substring(temp.lastIndexOf(".") + 1);
    }

    // Check each type of method
    public boolean isValidMethod(String method) {
        switch (method) {
            case "PUT":
                sendErrorCode(501);
                return false;
            case "DELETE":
                sendErrorCode(501);
                return false;
            case "LINK":
                sendErrorCode(501);
                return false;
            case "UNLINK":
                sendErrorCode(501);
                return false;
            case "GET":
                return true;
            case "HEAD":
                return true;
            case "POST":
                return true;
            default:
                sendErrorCode(400);
                return false;
        }
    }

    // Check if the file is valid. 403 for unreadable, 404 for not found
    public boolean isValidFile(String file) {

        File f = new File(file);
        Path p = Paths.get(file);
        if (f.exists()) {
            if (Files.isReadable(p)) {
                return true;
            } else {
                sendErrorCode(403);
                return false;
            }
        } else {
            sendErrorCode(404);
            return false;
        }
    }

    /*
     * These are the error codes: 200 OK 204 No Content 304 Not Modified 400 Bad
     * Request 403 Forbidden 404 Not Found 405 Method Not Allowed 408 Request
     * Timeout 411 Length Required 500 Internal Server Error 501 Not Implemented 503
     * Service Unavailable 505 HTTP Version Not Supported
     * 
     * Function should take in a codet and print its corresponding message: FORMAT:
     * - String message = String.format("HTTP/1.0 %s %s", code, ErrorMessage);
     * 
     */
    public void sendErrorCode(int code) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        String next_year = dateFormat.format(new Date(System.currentTimeMillis() + 13500000000L));
        String ErrorMessage = "";
        switch (code) {
            case 200:
                ErrorMessage = "OK";
                break;
            case 204:
                ErrorMessage = "No Content";
                break;
            case 304:
                ErrorMessage = "Not Modified" + "\r\n" + "Expires: " + next_year;
                break;
            case 400:
                ErrorMessage = "Bad Request";
                break;
            case 403:
                ErrorMessage = "Forbidden";
                break;
            case 404:
                ErrorMessage = "Not Found";
                break;
            case 405:
                ErrorMessage = "Method Not Allowed";
                break;
            case 408:
                ErrorMessage = "Request Timeout";
                break;
            case 411:
                ErrorMessage = "Length Required";
                break;
            case 500:
                ErrorMessage = "Internal Server Error";
                break;
            case 501:
                ErrorMessage = "Not Implemented";
                break;
            case 503:
                ErrorMessage = "Service Unavailable";
                break;
            case 505:
                ErrorMessage = "HTTP Version Not Supported";
                break;
        }

        // Format the header message.
        String message = String.format("HTTP/1.0 %s %s", code, ErrorMessage);
        try {
            System.out.println(message);
            outToClient.writeBytes(message);
            outToClient.flush();

        } catch (IOException e) {
            System.out.printf("Error sending response to CLIENT");
        }
        close();
    }

    // HANDLES 200 and 304 REQUESTS ONLY -> Gets called when all checks were
    // validated.
    public void handleRequest(String method, String file, int code) {
        String message = "";
        if (code == 200)
            message = String.format("HTTP/1.0 %s %s", 200, "OK");
        if (code == 304)
            message = String.format("HTTP/1.0 %s %s", 304, "Not Modified");
        
        try {
            outToClient.writeBytes(message + "\r\n" + createHeader(file) + "\r\n" + "\r\n");
            outToClient.flush();
        } catch (IOException e) {
            System.out.printf("Error sending response to client");
        }
        
        try {
            File new_file = new File(file);
            byte[] fileContent = Files.readAllBytes(new_file.toPath());
            if(file.equals("./index_seen.html"))
            {
                String fileContentsStr = new String(fileContent, StandardCharsets.UTF_8);
                fileContentsStr = fileContentsStr.replace("%YEAR-%MONTH-%DAY %HOUR-%MINUTE-%SECOND", lastSeen);
                fileContent = fileContentsStr.getBytes();
            }
            outToClient.write(fileContent);
            System.out.println("wrote to client");
            outToClient.flush();
        } catch (IOException e) {
            System.out.println("it broke");
        }
        close();

        // "file" is a string from the client request.
        // if ((method.equals("GET") || method.equals("POST")) && code == 200) {

        //     try {
        //         // String ext = getExtension(file);
        //         if (method.equals("POST")) {
        //             ProcessBuilder pb = new ProcessBuilder("." + file);
        //             Map<String, String> env = pb.environment();
        //             try {
        //                 env.put("CONTENT_LENGTH", CONTENTLENGTH);
        //                 env.put("SCRIPT_NAME", file);
        //                 env.put("HTTP_FROM", FROM);
        //                 env.put("HTTP_USER_AGENT", USERAGENT);
        //             } catch (Exception e) {
        //                 sendErrorCode(500);
        //             }
        //             Process process = pb.start();

        //             OutputStream out = process.getOutputStream();
        //             out.write(PARAMETERS.getBytes());
        //             out.flush();
        //             out.close();

        //             InputStream in = process.getInputStream();
        //             BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
        //             String line = "";
        //             String response = "";

        //             while ((response = buffer.readLine()) != null) {
        //                 line += response + "\n";
        //             }

        //             response = line;
        //             if (response == "") {
        //                 sendErrorCode(204);
        //             }
        //             in.close();

        //             cgi_out = response.getBytes();
        //             try {
        //                 outToClient.writeBytes(message + "\r\n" + createHeader(file, method) + "\r\n" + "\r\n");
        //                 outToClient.flush();

        //             } catch (IOException e) {
        //                 System.out.printf("Error to the client");
        //             }

        //             // write payload
        //             outToClient.write(cgi_out);
        //             outToClient.flush();

        //         } else if (method.equals("GET") || method.equals("HEAD")) {
        //             try {
        //                 outToClient.writeBytes(message + "\r\n" + createHeader(file, method) + "\r\n" + "\r\n");
        //                 outToClient.flush();

        //             } catch (IOException e) {
        //                 System.out.printf("Error to client");
        //             }
        //             File new_file = new File("." + file);
        //             byte[] fileContent = Files.readAllBytes(new_file.toPath());
        //             outToClient.write(fileContent);
        //             outToClient.flush();
        //         }

        //     } catch (IOException e) {
        //         sendErrorCode(500);
        //         System.out.println("it doesn't work");
        //     }
        // }
        //close();
    }

    // public boolean checkPayload(String file) {
    //     return true;
    // }

    // Assembles the header
    /*
     * FORMAT FOR REFERENCE | REQUEST EXAMPLE: GET resouces/bitcoin.pdf HTTP/1.0
     * HTTP/1.0 200 OK[CRLF] Content-Type: application/pdf[CRLF] Content-Length:
     * 184292[CRLF] Last-Modified: Tue, 14 Jul 2015 14:13:49 GMT[CRLF]
     * Content-Encoding: identity[CRLF] Allow: GET, HEAD, POST[CRLF] Expires: Fri,
     * 01 Oct 2021 03:44:00 GMT[CRLF] [CRLF]
     */

    private String createHeader(String fileName) {

        String header = "", extension, MIME = "";

        extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        // Mime Types:
        if (extension.equals("pdf")) {
            MIME = "application/pdf";
        } else if (extension.equals("gzip")) {
            MIME = "application/x-gzip";
        } else if (extension.equals("zip")) {
            MIME = "application/zip";
        } else if (extension.equals("txt")) {
            MIME = "text/plain";
        } else if (extension.equals("html")) {
            MIME = "text/html";
        } else if (extension.equals("gif")) {
            MIME = "image/gif";
        } else if (extension.equals("jpg")) {
            MIME = "image/jpeg";
        } else if (extension.equals("png")) {
            MIME = "image/png";
        } else {
            MIME = "application/octet-stream";
        }

        File new_file = new File(fileName);
        long fileSize = new_file.length();
        header += "Content-Type: " + MIME + "\r\n";
        header += "Content-Length: " + fileSize + "\r\n";

        SimpleDateFormat dateSimpleFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        dateSimpleFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        header += "Last-Modified: " + dateSimpleFormat.format(new_file.lastModified()) + "\r\n";
        header += "Content-Encoding: identity" + "\r\n";
        header += "Allow: GET, POST, HEAD" + "\r\n";

        long currentTime = System.currentTimeMillis();
        // Date currentDate = new Date(currentTime + 31540000000L);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);
        header += "Expires: " + dateSimpleFormat.format(cal.getTime()) + "\r\n";

        SimpleDateFormat cookieDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String goodCurrentDate = cookieDate.format(new Date(currentTime));
        header += "Set-Cookie: lasttime=" + URLEncoder.encode(goodCurrentDate, StandardCharsets.UTF_8);

        return header;
    }

    /*
     * Void method for organization Just closes the socket.
     */
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        }
    }
}
