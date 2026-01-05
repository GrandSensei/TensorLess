import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class PredictorServer {

    public static void main(String[] args) {
        int port = 9999;
        System.out.println("----------------------------------------");
        System.out.println("🚀 NEURAL SERVER STARTING ON PORT " + port);
        System.out.println("----------------------------------------");

        try {
            // 1. LOAD THE MODEL ONCE
            System.out.println("Loading Neural Network Model...");
            NeuralEngine nn = NeuralEngine.loadModel("java_core/bin/model.bin");
            System.out.println("✅ Model Loaded! Ready for predictions.");

            // 2. OPEN THE SERVER
            ServerSocket serverSocket = new ServerSocket(port);

            // 3. ENTER THE INFINITE LOOP
            while (true) {
                // Wait right here until Python connects
                Socket clientSocket = serverSocket.accept();

                // Handle the request
                handleClient(clientSocket, nn);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket, NeuralEngine nn) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // 1. Read the pixels sent by Python
           String inputLine = in.readLine();




            // If Render sends a standard HTTP health check, ignore it silently.
           if (inputLine.startsWith("HEAD") || inputLine.startsWith("GET") || inputLine.startsWith("POST")) {
                //System.out.println("Health check received.");
               return; // Skip the rest of the loop and wait for the next request
            }

            if (inputLine == null) return;

            // 2. Parse Data (String "0,0,255..." -> float[])
            String[] stringValues = inputLine.split(",");
            float[] inputs = new float[784];
            for (int i = 0; i < 784; i++) {
                inputs[i] = Float.parseFloat(stringValues[i]);
            }

            // 3. Run Prediction
            nn.setInput(inputs);
            nn.forwardPass();

            // 4. Send "Confidences" back
            // We have to capture the print output or manually format it string again
            StringBuilder confidences = new StringBuilder("CONFIDENCES:");
            var outputLayer = nn.neuralNetwork.getNeurons().getLast();
            for (int i = 0; i < outputLayer.size(); i++) {
                confidences.append(outputLayer.get(i).getActivation());
                if (i < outputLayer.size() - 1) confidences.append(",");
            }
            out.println(confidences.toString());

            // 5. Send Result back
            int result = nn.getPredictedDigit();
            out.println("PREDICTION_RESULT:" + result);

            System.out.println("[Java-side] Processed request. Result: " + result);

        } catch (Exception e) {
            System.err.println("[Java-side] Error handling client: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) { /* ignore */ }
        }
    }
}
