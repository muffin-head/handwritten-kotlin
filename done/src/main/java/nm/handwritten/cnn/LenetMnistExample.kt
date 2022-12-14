package nm.handwritten.cnn

import nm.handwritten.cnn.LenetMnistExample
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.LearningRatePolicy
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.Updater
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.activations.Activation
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception
import java.util.HashMap


object LenetMnistExample {
    private val log = LoggerFactory.getLogger(LenetMnistExample::class.java)
    private const val ouput = ""
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val nChannels = 1 // Number of input channels
        val outputNum = 10 // The number of possible outcomes
        val batchSize = 64 // Test batch size
        val nEpochs = 100 // Number of training epochs
        val iterations = 1 // Number of training iterations
        val seed = 123 //

        /*
            Create an iterator using the batch size for one iteration
         */log.info("Load data....")
        val mnistTrain: DataSetIterator = MnistDataSetIterator(batchSize, true, 12345)

        /*
            Construct the neural network
         */log.info("Build model....")

        // learning rate schedule in the form of <Iteration #, Learning Rate>
        val lrSchedule: MutableMap<Int, Double> = HashMap()
        lrSchedule[0] = 0.01
        lrSchedule[1000] = 0.005
        lrSchedule[3000] = 0.001
        val conf = NeuralNetConfiguration.Builder()
            .seed(seed)
            .iterations(iterations) // Training iterations as above
            .regularization(true).l2(0.0005) /*
                    Uncomment the following for learning decay and bias
                 */
            .learningRate(.01) //.biasLearningRate(0.02)
            /*
                    Alternatively, you can use a learning rate schedule.
                    NOTE: this LR schedule defined here overrides the rate set in .learningRate(). Also,
                    if you're using the Transfer Learning API, this same override will carry over to
                    your new model configuration.
                */
            .learningRateDecayPolicy(LearningRatePolicy.Schedule)
            .learningRateSchedule(lrSchedule) /*
                    Below is an example of using inverse policy rate decay for learning rate
                */
            //.learningRateDecayPolicy(LearningRatePolicy.Inverse)
            //.lrPolicyDecayRate(0.001)
            //.lrPolicyPower(0.75)
            .weightInit(WeightInit.XAVIER)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(Updater.NESTEROVS) //To configure: .updater(new Nesterovs(0.9))
            .list()
            .layer(0,
                ConvolutionLayer.Builder(5,
                    5) //nIn and nOut specify depth. nIn here is the nChannels and nOut is the number of filters to be applied
                    .nIn(nChannels)
                    .stride(1, 1)
                    .nOut(20)
                    .activation(Activation.IDENTITY)
                    .build())
            .layer(1, SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                .kernelSize(2, 2)
                .stride(2, 2)
                .build())
            .layer(2, ConvolutionLayer.Builder(5, 5) //Note that nIn need not be specified in later layers
                .stride(1, 1)
                .nOut(50)
                .activation(Activation.IDENTITY)
                .build())
            .layer(3, SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                .kernelSize(2, 2)
                .stride(2, 2)
                .build())
            .layer(4, DenseLayer.Builder().activation(Activation.RELU)
                .nOut(500).build())
            .layer(5, OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nOut(outputNum)
                .activation(Activation.SOFTMAX)
                .build())
            .setInputType(InputType.convolutionalFlat(28, 28, 1)) //See note below
            .backprop(true).pretrain(false).build()

        /*
        Regarding the .setInputType(InputType.convolutionalFlat(28,28,1)) line: This does a few things.
        (a) It adds preprocessors, which handle things like the transition between the convolutional/subsampling layers
            and the dense layer
        (b) Does some additional configuration validation
        (c) Where necessary, sets the nIn (number of input neurons, or input depth in the case of CNNs) values for each
            layer based on the size of the previous layer (but it won't override values manually set by the user)
        InputTypes can be used with other layer types too (RNNs, MLPs etc) not just CNNs.
        For normal images (when using ImageRecordReader) use InputType.convolutional(height,width,depth).
        MNIST record reader is a special case, that outputs 28x28 pixel grayscale (nChannels=1) images, in a "flattened"
        row vector format (i.e., 1x784 vectors), hence the "convolutionalFlat" input type used here.
        */
        val model = MultiLayerNetwork(conf)
        model.init()
        log.info("Train model....")
        model.setListeners(ScoreIterationListener(1))
        var mnistTest: DataSetIterator? = null
        for (i in 0 until nEpochs) {
            model.fit(mnistTrain)
            log.info("*** Completed epoch {} ***", i)
            if (mnistTest == null) {
                mnistTest = MnistDataSetIterator(10000, false, 12345)
            }
            log.info("Evaluate model....")
            val eval = model.evaluate(mnistTest)
            if (eval.accuracy() >= 0.9901) {
                val locationToSave =
                    File(ouput) //Where to save the network. Note: the file is in .zip format - can be opened externally
                val saveUpdater =
                    true //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
                ModelSerializer.writeModel(model, locationToSave, saveUpdater)
                log.info("found ")
                break
            }
            log.info(eval.stats())
            mnistTest.reset()
        }
        log.info("****************Example finished********************")
    }
}