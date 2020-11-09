package sdns.app.utils;

class ValidationUtils {
    //Max port available
    private static final int MAX_PORT = 65535;

    /**
     * Get the port from the argument
     * @param port to check and parse
     * @return port
     * @throws IllegalArgumentException if bad port
     */
    public static int getPort(String port) throws IllegalArgumentException {
        //get server port and validate
        try {
            int serverPort = Integer.parseInt(port);
            if(serverPort < 0 || serverPort > MAX_PORT){
                throw new IllegalArgumentException("ERROR: Port must be [0, " + MAX_PORT + "]");
            }
            return serverPort;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ERROR: Malformed port: \"" + port + "\"");
        }
    }
}
