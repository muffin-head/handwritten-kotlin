package ramo.klevis

import org.slf4j.LoggerFactory
import ramo.klevis.ui.ProgressBar
import javax.swing.JFrame
import kotlin.Throws
import kotlin.jvm.JvmStatic
import ramo.klevis.ui.UI
import java.lang.Exception
import java.util.concurrent.Executors
import java.lang.Runnable

/**
 * Created by klevis.ramo on 11/24/2017.
 */
object Run {
    private val LOGGER = LoggerFactory.getLogger(Run::class.java)
    private val mainFrame = JFrame()
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        LOGGER.info("Application is starting ... ")
        val progressBar = ProgressBar(mainFrame, true)
        progressBar.showProgressBar("Collecting data this make take several seconds!")
        val ui = UI()
        Executors.newCachedThreadPool().submit {
            try {
                ui.initUI()
            } finally {
                progressBar.setVisible(false)
                mainFrame.dispose()
            }
        }
    }
}