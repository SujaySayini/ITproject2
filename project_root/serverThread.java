
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
    BufferedReader inFromClient;
    DataOutputStream outToClient;
    String lastCookie;

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
            // System.out.println("");
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
        // String modified = "";

        if (file.equals("/"))
            file = "./index.html";
        else
            file = "." + file;

        // will find cookie header
        // put post = stringBuffer.toString().split("\r\n"); in main
        // put String cookie = "";
        // get file from file = requestParts[1];

        String cookie_value = "";
        if (Content.length == 4) {
            if (Content[3].contains("Cookie")) {
                cookie_value = Content[3].split(":")[1].trim();

                // if Cookie is already made and is correct date and format, change value in index_seen.html as file
                if (ifValidCookie(cookie_value)) {
                    //set last cookie 
                    try {
                        String decoded = URLDecoder.decode(cookie_value, "UTF-8");
                        String fullYear = decoded.split(" ")[0];
                        String fullTime = decoded.split(" ")[1];
            
                        lastCookie = fullYear + " " + fullTime;
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    
                    file = "./index_seen.html";
                }
            }
        }

        if (!isValidMethod(requestParts[0]))
            return;

        // System.out.println(file);
        // check if file is valid
        if (isValidFile(file))
            handleRequest(method, file, 200);
    }

    public boolean ifValidCookie(String cookie_value) {

        boolean ifValid = false;
        try {
            String cookie_decoded = URLDecoder.decode(cookie_value, "UTF-8");
            if (cookie_decoded.split("=")[0].equals("lasttime")) {
                // check for valid date format:
                String value =  cookie_decoded.split("=")[1];
                Date myDate = null;
                Date now = new Date();
                try {
                    SimpleDateFormat dateSimple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    myDate = dateSimple.parse(value);
                    
                    if (!dateSimple.parse(dateSimple.format(now)).after(dateSimple.parse(value)) || !value.equals(dateSimple.format(myDate)))
                        myDate = null;
                } catch (ParseException ex) {
                    ex.printStackTrace();
                }
                if (myDate != null)
                    ifValid = true;
                else
                    ifValid = false;
            } else
                ifValid = false;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            ifValid = false;
        }

        return ifValid;

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
                // not readable and forbidden
                sendErrorCode(403);
                return false;
            }
        } else {
            //can't find it 
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
            System.out.printf("Error sending to CLIENT");
        }
        close();
    }

    // HANDLES 200 and 304 REQUESTS ONLY -> Gets called when all checks were validated.
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
            System.out.printf("Error sending to client");
        }

        try {
            File new_file = new File(file);
            byte[] fileContent = Files.readAllBytes(new_file.toPath());
            if (file.equals("./index_seen.html")) {
                String str_content = new String(fileContent, StandardCharsets.UTF_8);
                str_content = str_content.replace("%YEAR-%MONTH-%DAY %HOUR-%MINUTE-%SECOND", lastCookie);
                fileContent = str_content.getBytes();
            }
            outToClient.write(fileContent);
            outToClient.flush();
        } catch (IOException e) {
            System.out.println("Error2");
        }
        close();
    }

    // Assembles the header
    /*
     * FORMAT FOR REFERENCE | REQUEST EXAMPLE: GET resouces/bitcoin.pdf HTTP/1.0
     *      HTTP/1.0 200 OK[CRLF]
            Content-Type: application/pdf[CRLF]
            Content-Length: 184292[CRLF]
            Last-Modified: Tue, 14 Jul 2015 14:13:49 GMT[CRLF]
            Content-Encoding: identity[CRLF]
            Allow: GET, HEAD, POST[CRLF]
            Expires: Fri, 01 Oct 2021 03:44:00 GMT[CRLF]
            [CRLF]
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
        SimpleDateFormat dateSimpleFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");

        header += "Content-Type: " + MIME + "\r\n";
       
        if (fileName.equals("./index_seen.html")) 
            fileSize -= dateSimpleFormat.toString().length();
    
        header += "Content-Length: " + fileSize + "\r\n";

        dateSimpleFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        header += "Last-Modified: " + dateSimpleFormat.format(new_file.lastModified()) + "\r\n";
        header += "Content-Encoding: identity" + "\r\n";
        header += "Allow: GET, POST, HEAD" + "\r\n";

        long currentTime = System.currentTimeMillis();
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);
        header += "Expires: " + dateSimpleFormat.format(cal.getTime()) + "\r\n";

        SimpleDateFormat cookieDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strCookiedate = cookieDate.format(new Date(currentTime));
        header += "Set-Cookie: lasttime=" + URLEncoder.encode(strCookiedate, StandardCharsets.UTF_8);

        return header;
    }

    //close socket
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        }
    }
}
