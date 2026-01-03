import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class NeuralEngine {



    private NeuralNetwork neuralNetwork;

    private float[][] trainingData;
    private List<List<Float>> testData;

    private Weights modelWeights;

    private final int BATCH_SIZE= 1000;
    private final int LAYERS = 4;
    private final int INPUT_SIZE =784;
    private final int OUTPUT_SIZE =10;
    private final float LEARNING_RATE = 0.01f;
    private final int STANDARD =0;

    public NeuralEngine() {
        neuralNetwork = new NeuralNetwork(LAYERS);
        // This creates a 4 layer neural network with one input layer, 2 hidden layers and one output layer.
        setLayer(0,INPUT_SIZE);
        setLayer(1,128);
        setLayer(2,64);
        setLayer(3,OUTPUT_SIZE);
        System.out.println("The neural network has been set");
        setWeights();
        System.out.println("The weights have been set(randomized)");
    }

    public NeuralEngine(int layers) {
        if(layers == STANDARD) setNeuralNetwork(LAYERS);
        else  setNeuralNetwork(layers);
    }


    //works imo
    public void setNeuralNetwork(int layers) {
        neuralNetwork = new NeuralNetwork(layers);
        if(layers>0) setLayer(0,INPUT_SIZE);
        if(layers>1) setLayer(layers-1,OUTPUT_SIZE);
        if(layers==STANDARD){
            setLayer(1,16);
            setLayer(2,16);

        }
        System.out.println("The neural network has been set");
        setWeights();
        System.out.println("The weights have been set");
        System.out.println("The neural network is ready");
        System.out.println("Number of layers: " +neuralNetwork.getNeurons().size());
    }



    //use this when building a custom network
    public void setLayer(int layerIndex, int noOfNeurons) {
        if(layerIndex>=neuralNetwork.getNeurons().size()) return;
        //Clear existing neurons in the layer if any
        neuralNetwork.getNeurons().get(layerIndex).clear();
        for(int z=0; z<noOfNeurons;++z) {
            Neuron neuron = new Neuron();
            neuralNetwork.addNeuron(layerIndex,neuron);
           // System.out.println("Adding neuron with bias: "+neuron.getBias() + " to layer "  + layerIndex);
        }
      //  System.out.println("number of neurons in layer " + layerIndex + " is " + noOfNeurons);
    }


    public void setWeights(){
        // initialize the weights
        neuralNetwork.setWeights(new Weights(new java.util.ArrayList<>()));


        int layers  = neuralNetwork.getNeurons().size();
        // we need weight between two layers (midLayer= 0 -> weights between layer 0 and layer 1)
        for(int midLayer = 0 ; midLayer<layers-1;++midLayer){

            neuralNetwork.getWeights().addMidLayer();   //adds a layer of weights in the weight list
            //neurons from one layer
            int neuronsInCurrentLayer = neuralNetwork.getNeurons().get(midLayer).size();

            //neurons from the next layer
            int neuronsInNextLayer = neuralNetwork.getNeurons().get(midLayer+1).size();

            //for every neuron in the current layer
            for(int neuronFrom=0;neuronFrom<neuronsInCurrentLayer;++neuronFrom){
                List<Float> connections = new ArrayList<>();

                double range = 1.0 / Math.sqrt(neuronsInCurrentLayer);
                //for every neuron in the next layer
                for(int neuronTo=0;neuronTo<neuronsInNextLayer;++neuronTo){
                    //randomize the weights between -range and range
                    float weight = (float) (Math.random()*2*range-range);
                    connections.add(weight);
                }
                neuralNetwork.getWeights().getWeightsOfLayer(midLayer).add(connections);
            }
        }

    }



    // forward propagation
    public void forwardPass() {

        //System.out.println(neuralNetwork.getNeurons().size());

        //I am just calling the variables again for more readability
        List<List<Neuron>> layers = neuralNetwork.getNeurons();
        Weights weights = neuralNetwork.getWeights();

        //Start from layer 0, move data to layer 1, then continue the loop
        for(int prevLayer=0;prevLayer<neuralNetwork.getNeurons().size()-1;++prevLayer){
                List<Neuron> currentLayerNeurons = layers.get(prevLayer);
                List<Neuron> nextLayerNeurons = layers.get(prevLayer+1);
            for (int nextNeuron = 0; nextNeuron < nextLayerNeurons.size(); ++nextNeuron) {
                float sum = nextLayerNeurons.get(nextNeuron).getBias();

                for (int currentNeuron = 0; currentNeuron < currentLayerNeurons.size(); ++currentNeuron) {

                    float input = currentLayerNeurons.get(currentNeuron).getActivation();
                    float weight = weights.getWeight(prevLayer, currentNeuron, nextNeuron);
                    sum += weight * input;
                }
                 nextLayerNeurons.get(nextNeuron).setVal(sum);
            }
        }

        //make it return the output neurons for debugging later ig
    }




    //turns out it is simpler to just do it in the training method itself here. I will switch this in python one though.
//populate the inputs!
    public void setInput(float[] inputs){
        if(inputs.length!=INPUT_SIZE) return;
        for(int i=0;i<INPUT_SIZE;++i){
            neuralNetwork.getNeuron(0,i).activate(inputs[i]);
        }
        System.out.println("Inputs have been set");
//        for(int x= 0; x<INPUT_SIZE;++x){
//            System.out.println("Input " + x + " is " + neuralNetwork.getNeuron(0,x).getVal());
//        }
    }


    public void train(int epochs){

        // initialize the weights that are needed to be modified
    for (int e = 0; e < epochs; e++) {
        int iterations = trainingData.length / BATCH_SIZE;
        //no of the partitions you made of the data
        for (int i = 0; i < iterations; ++i) {


            float cost = 0;
            // the data in the given partition
            int start = i * BATCH_SIZE;
            //first populate the inputs
            for (int j = start; j < start + BATCH_SIZE; ++j) {
                float[] inputs = trainingData[j];
                List<Neuron> inputLayer = neuralNetwork.getNeurons().getFirst();
                // k = 1 as the 0th index is the label
                for (int k = 1; k < trainingData[j].length; ++k) {
                    //set the input layer of neurons
                    float input = inputs[k];
                    inputLayer.get(k - 1).setVal(input);
                }

            // do the forward pass once you have set the inputs
            forwardPass();

            cost += calculateCostFunction(j);

            float label = inputs[0];
            float[] targets = new float[OUTPUT_SIZE];
            for (int x = 0; x < OUTPUT_SIZE; ++x) {
                if (x == label) targets[x] = 1.0f;
                else targets[x] = 0.f;
            }

            //We calculate the changes we want to make to each weight from the cost function we just made above.
            backpropagate(targets);
            if (j % 1000 == 0) {
                System.out.print("Target: " + label + " | Prediction: ");
                List<Neuron> out = neuralNetwork.getNeurons().getLast();
                int bestGuess = 0;
                float bestVal = 0;
                for(int k=0; k<10; k++) {
                    // Print the raw output to see if they are all 0.5 or 0.0
                    // System.out.print(String.format("%.2f ", out.get(k).getActivation()));
                    if(out.get(k).getActivation() > bestVal) {
                        bestVal = out.get(k).getActivation();
                        bestGuess = k;
                    }
                }
                System.out.println("-> " + bestGuess);
            }
        }


        cost /= BATCH_SIZE;
        System.out.println("The cost is " + cost);
    }
    ++e;
}

    }

    // Returns the index (0-9) of the neuron with the highest activation
    public int getPredictedDigit() {
        List<Neuron> outputLayer = neuralNetwork.getNeurons().getLast();
        int bestIndex = -1;
        float highestValue = -1f;

        for (int i = 0; i < outputLayer.size(); i++) {
            float activation = outputLayer.get(i).getActivation();
            if (activation > highestValue) {
                highestValue = activation;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    public void test(float[][] testData) {
        System.out.println("----------------------------------");
        System.out.println("Running Test on " + testData.length + " examples...");

        int correctGuesses = 0;

        for (int i = 0; i < testData.length; i++) {
            float[] row = testData[i];

            // 1. SETUP INPUTS (Same logic as training)
            // row[0] is the label, row[1]...row[784] are inputs
            List<Neuron> inputLayer = neuralNetwork.getNeurons().getFirst();
            for(int k=1; k < row.length; ++k){
                // Remember: k-1 because neuron index starts at 0
                inputLayer.get(k-1).setActivation(row[k]);
            }

            // 2. FORWARD PASS ONLY (No Backprop)
            forwardPass();

            // 3. CHECK ANSWER
            int actualLabel = (int) row[0];
            int predictedLabel = getPredictedDigit();

            if (actualLabel == predictedLabel) {
                correctGuesses++;
            }

            // Optional: Print every 1000th test to show it's alive
            if (i % 1000 == 0) {
                System.out.println("Test #" + i + ": Act=" + actualLabel + ", Pred=" + predictedLabel);
            }
        }

        // 4. CALCULATE SCORE
        double accuracy = (double) correctGuesses / testData.length * 100;
        System.out.println("----------------------------------");
        System.out.println("FINAL ACCURACY: " + String.format("%.2f", accuracy) + "%");
        System.out.println("Correct: " + correctGuesses + "/" + testData.length);
        System.out.println("----------------------------------");
    }

    public void backpropagate(float[] targets){
            List<List<Neuron>> layers = neuralNetwork.getNeurons();
            Weights weights = neuralNetwork.getWeights();

            //We will store the errors of the current layer processed
            float[] errors = new float[OUTPUT_SIZE];

            //calculate the output layer error
            int outputLayerIndex = layers.size() - 1;
            // System.out.println(outputLayerIndex);
            List<Neuron> outputLayer = layers.get(outputLayerIndex);

            for (int x = 0; x < OUTPUT_SIZE; ++x) {
                Neuron neuron = outputLayer.get(x);
                float error = (neuron.getActivation() - targets[x]);
                errors[x] = error;

                //update the bias
                neuron.setBias(neuron.getBias() - LEARNING_RATE * error);
            }


            // calculate the errors in hidden layers
            for (int midLayer = layers.size() - 2; midLayer >= 0; --midLayer) {
                List<Neuron> currentLayerNeurons = layers.get(midLayer);
                List<Neuron> nextLayerNeurons = layers.get(midLayer + 1);

                // new array to store the errors of the current layer
                float[] currentLayerErrors = new float[currentLayerNeurons.size()];
                for (int currentNeuron = 0; currentNeuron < currentLayerNeurons.size(); ++currentNeuron) {
                    Neuron currentLayerNeuron = currentLayerNeurons.get(currentNeuron);

                    float errorSum = 0;

                    // calculate the gradient based on layer ahead
                    for (int nextNeuron = 0; nextNeuron < nextLayerNeurons.size(); ++nextNeuron) {
                        float weight = weights.getWeight(midLayer, currentNeuron, nextNeuron);

                        //the error sum that came from the coming layer
                        errorSum += weight * errors[nextNeuron];

                        //update the weight
                        float gradient = errors[nextNeuron] * currentLayerNeuron.getActivation();
                        float updatedWeight = weight - LEARNING_RATE * gradient;
                        weights.setWeight(midLayer, currentNeuron, nextNeuron, updatedWeight);

                    }

                    // calculate error for this neuron for layer before it
                    currentLayerErrors[currentNeuron] = errorSum * currentLayerNeuron.getSigmoidDerivative();

                    if (midLayer > 0) {
                        float newBias = currentLayerNeuron.getBias() - LEARNING_RATE * currentLayerErrors[currentNeuron];
                        currentLayerNeuron.setBias(newBias);
                    }


                }
                errors = currentLayerErrors;
            }


    }



    //the inputs range is not -1,1 so be careful to parse that later

    //This is for the MNIST dataset
    private float calculateCostFunction(int trainingDataIndex){
        float cost = 0;
        float[] trainingData = this.trainingData[trainingDataIndex];
        //Make an array of the output neurons
        float[] target = new float[OUTPUT_SIZE];
        for(int i=0;i<OUTPUT_SIZE;++i){
            if(i==trainingData[0]) target[i]=1;
            else target[i]=0;
        }
        //
        List<Neuron> outputLayer = neuralNetwork.getNeurons().getLast();

        for(int x = 0; x<outputLayer.size();++x){
            float output = outputLayer.get(x).getActivation();
            // we subtract the output from the target,
            // we square it
            cost += (output-target[x])*(output-target[x]);
            // we sum it up

        }
        // we divide by the batch size
        return cost;
    }




    public void setTrainingData(float[][] trainingData) {
        this.trainingData = trainingData;
    }





    // SAVING THE MODEL AFTER TRAINING

    public void saveModel(String filePath) throws FileNotFoundException {
        System.out.println("Saving model to " + filePath);
        try(DataOutputStream dos = new DataOutputStream(new FileOutputStream(filePath))){

            // start storing number of layers
            List<List<Neuron>> layers = neuralNetwork.getNeurons();
            dos.writeInt(layers.size());

            for (List<Neuron> layer : layers) {
                dos.writeInt(layer.size());
                for (Neuron neuron : layer) {
                    //we only save the bias.
                    dos.writeFloat(neuron.getBias());
                }
            }

            Weights weights = neuralNetwork.getWeights();

            for (int x=0;x<layers.size()-1;++x){
                int currentLayerSize = layers.get(x).size();
                int nextLayerSize = layers.get(x+1).size();
                for (int from=0;from<currentLayerSize;++from){
                    for (int to=0;to<nextLayerSize;++to){
                        dos.writeFloat(weights.getWeight(x,from,to));
                    }
                }
            }

            System.out.println("Model saved!!");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving model");
        }
    }

    // LOADING THE MODEL AFTER TRAINING
    public static NeuralEngine loadModel(String filePath) throws IOException {
        System.out.println("Loading model from " + filePath);


        try(DataInputStream dis = new DataInputStream(new FileInputStream(filePath))){
            //the first thing we read is the number of layers
            int numberOfLayers = dis.readInt();

            //HERE THERE IS REDUNDANCY WITH THE CONSTRUCTOR BECAUSE WE ARE CREATING THE NEURAL NETWORK HERE
            //MAKE BETTER CONSTRUCTORS...
            NeuralEngine engine = new NeuralEngine(0);
            engine.neuralNetwork= new NeuralNetwork(numberOfLayers);

            // Do the biases !
            for (int x=0;x<numberOfLayers;++x){
                int numberOfNeurons = dis.readInt();

                // create the layer
                engine.setLayer(x,numberOfNeurons);
                //remind myself to create an empty constructor for this to prevent redunduncy
                for (int y=0;y<numberOfNeurons;++y){
                    float bias = dis.readFloat();
                    engine.neuralNetwork.getNeuron(x,y).setBias(bias);
                }
            }

            //Now focus on the weights

            engine.setWeights();

            //override these from the file
            Weights weights = engine.neuralNetwork.getWeights();
            List<List<Neuron>> layers = engine.neuralNetwork.getNeurons();
            for (int x=0;x<numberOfLayers-1;++x){
                int currentLayerSize = layers.get(x).size();
                int nextLayerSize = layers.get(x+1).size();
                for (int from=0;from<currentLayerSize;++from){
                    for (int to=0;to<nextLayerSize;++to){
                        float weight = dis.readFloat();
                        weights.setWeight(x,from,to,weight);
                    }
                }
            }

            System.out.println("Model loaded!!");
            return engine;
        }

    }


}
