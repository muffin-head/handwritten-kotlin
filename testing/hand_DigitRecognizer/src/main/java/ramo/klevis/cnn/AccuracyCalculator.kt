package ramo.klevis.cnn

import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.deeplearning4j.earlystopping.scorecalc.ScoreCalculator
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.slf4j.LoggerFactory
import ramo.klevis.cnn.AccuracyCalculator

/**
 * Created by klevis.ramo on 12/13/2017.
 */
class AccuracyCalculator(private val dataSetIterator: MnistDataSetIterator) : ScoreCalculator<MultiLayerNetwork> {
    var i = 0
    override fun calculateScore(network: MultiLayerNetwork): Double {
        val evaluate = network.evaluate(dataSetIterator)
        val accuracy = evaluate.accuracy()
        log.error("Accuracy " + i++ + " " + accuracy)
        return 1 - evaluate.accuracy()
    }

    companion object {
        private val log = LoggerFactory.getLogger(AccuracyCalculator::class.java)
    }
}