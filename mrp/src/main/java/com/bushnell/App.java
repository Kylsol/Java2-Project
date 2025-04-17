package com.bushnell;

/**
 * Main application class.
 */
public final class App {
    // Private constructor to prevent instantiation
    private App() {
    }

    /**
     * Entry point of the application.
     * Prints the current working directory.
     * 
     * @param args Command-line arguments (not used here).
     */
    public static void main(String[] args) {
        System.out.println("Current working dir: " + System.getProperty("user.dir"));
    }
}
