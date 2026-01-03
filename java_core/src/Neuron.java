public class Neuron {

    private float val;

    private float activation;

    private float bias;

    public Neuron(float val) {
        this.val = val;
    }

    public float getVal() {
        return val;
    }

    public float getBias() {
        return bias;
    }

    public void setBias(float bias) {
        this.bias = bias;
    }

    public void setVal(float val) {
        this.val = val;
        activate(val);
    }
    public float getActivation() {return activation;}

    public void setActivation(float activatedValue){ this.activation=activatedValue;}
    public Neuron() {
        setVal(0);
        setRandomBias();

    }
    public void setRandomBias() {
        //the range is -1 to 1
        this.bias = (float) Math.random() * 2 - 1;
    }

    public void activate(float val){
        this.val = val;
        this.activation = (float)(1 /(1+Math.exp(-val)));
    }

    public float getSigmoidDerivative(){

        return (this.activation * (1 - this.activation));
    }


}
