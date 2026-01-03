import java.io.IOException;

public class Predictor {

    public static void main(String[] args) {
        //the idea is to use CLI to collect all the pixel values

        //Imagine the frontend manages how to get the pixel values and put a CLI command

        if(args.length<1){
            System.out.println("Error in the input");
            return;
        }

        try{
            NeuralEngine nn = NeuralEngine.loadModel("model.bin");
            String[] stringValues = args[0].split(",");
            float[] inputs = new float[784];
            for (int i=0;i<784;i++){
                inputs[i] = Float.parseFloat(stringValues[i]);
            }
            nn.setInput(inputs);

            nn.forwardPass();

            int result = nn.getPredictedDigit();

            System.out.println("PREDICTION_RESULT:" + result);

        } catch (IOException e) {
            System.out.println("Error loading the model");
            System.out.println(e.getMessage());

        }


    }
}
