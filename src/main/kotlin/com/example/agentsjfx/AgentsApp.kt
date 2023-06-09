package com.example.agentsjfx

import com.example.agentsjfx.AgentsApp.Companion.KMeans.Companion.cluster
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.hStep
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.providerPolicy
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.randCalcDist
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.randExpoD
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.reporterPolicy
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.sBiasP
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.sBiasR
import javafx.application.Application
import javafx.application.Platform
import javafx.embed.swing.SwingNode
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import jetbrains.datalore.plot.MonolithicCommon
import jetbrains.datalore.vis.swing.jfx.DefaultPlotPanelJfx
import org.apache.commons.math3.ml.clustering.CentroidCluster
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.apache.commons.math3.ml.distance.EuclideanDistance
import org.apache.commons.math3.random.MersenneTwister
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.intern.toSpec
import org.jetbrains.letsPlot.letsPlot
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt

class AgentsApp : Application() {
    
    override fun start(stage: Stage) {
        stage.title = "Agents-jfx"
        stage.scene = Scene(root, 1024.0, 768.0)
        stage.show()
    }

    companion object {
        val fxmlLoader = FXMLLoader(AgentsApp::class.java.getResource("main-view.fxml"))
        val root = fxmlLoader.load<BorderPane>()
        val data = LinkedList<Cycle>()
        var random = MersenneTwister(100)

        class Params(
            val N: Int,
            val S: Int,
            val NC: Int,
            val KMin: Int,
            val KMax: Int,
            val ExpoA: Double,
            val ExpoG: Double,
            val V0: Double,
            val X: Double,
            val Y: Double,
            val Z: Double,
            var sAgent: IntArray?
        ) {
            init{
                require(sAgent == null || sAgent!!.size == N){"Size of sAgent array must be N or null. 1 for strategic."}
                sAgent?:run {
                    sAgent = IntArray(N)
                    var inSA = 0
                    var rn = 0
                    while (inSA < S) {
                        rn = random.nextInt(0, N)
                        if (sAgent!![rn]== 0) {    // !! not null asserted call
                            sAgent!![rn] = 1
                            inSA += 1
                        }
                    }
                }
                println(sAgent!!.joinToString(separator = "|", prefix = "sAgent(${sAgent!!.count(){it==1}}):[", postfix = "]"))
            }
            override fun toString(): String {
                return "Params(N=$N, " +
                        "S=$S, " +
                        "NC=$NC, " +
                        "KMin=$KMin, " +
                        "KMax=$KMax, " +
                        "ExpoA=${"%.2f".format(ExpoA)}, " +
                        "ExpoG=${"%.2f".format(ExpoG)}, " +
                        "V0=${"%.2f".format(V0)}, " +
                        "X=${"%.2f".format(X)}, " +
                        "Y=${"%.2f".format(Y)}, " +
                        "Z=${"%.2f".format(Z)})"
            }
        }

        class MiscParams(
            val findClients: Boolean,   // false - Find providers for clients, true - Find clients for providers
            val symmetricalModel: Boolean,
            val useCalcDist: Boolean
        )

        class Cycle(
            val V: DoubleArray,
            val meanVs: Double,
            val meanVh: Double,
            val netOutflow: Double,
            val disc: BooleanArray,
            val sToSCoopNum: Int,
            val counterAllCoop: Int
        )

        class Policy {
            companion object {
                fun hStep(v: Double, x: Double): Double { // 1(v>=1-x)
                    return if (v >= 1 - x) 1.0 else .0
                }
                fun sBiasP(y: Double, L: Double): Double{ // p
                    return minOf(y, L)
                }
                fun sBiasR(z: Double, L: Double): Double{ // r
                    return minOf(z, L)
                }
                fun providerPolicy(A: Double, p: Double): Double{ // Pij
                    return minOf(A, p)
                }
                fun reporterPolicy(G: Double, P: Double, r: Double): Double{ // Rij
                    return minOf(G*P, r)
                }
                fun randExpoD(expo: Double): Double{
                    return Math.pow(random.nextDouble(), 1.0/expo)
                }

                fun randCalcDist(dist: FloatArray = floatArrayOf(0.008F, 0.043F, 0.1288F, 0.2805F, 0.4871F, 0.694F, 0.8546F, 0.9537F, 0.9898F, 1.0F)): Double{
                    val number = random.nextDouble()
                    var ret = .0
                    dist.forEachIndexed { index, fl ->
                        if(number<=fl){
                            ret = (index+1).toDouble()/dist.size.toDouble()
                            print("#${ret}")
                            return ret
                        }
                    }
                    print("#${ret}")
                    return ret
                }

            }
        }

        class KMeans{
            companion object {
                val distanceMeasure: DistanceMeasure = DistanceMeasure { a, b ->
                    EuclideanDistance().compute(doubleArrayOf(a[0]), doubleArrayOf(b[0]))
                }
                val clusterer: KMeansPlusPlusClusterer<DoublePoint> = KMeansPlusPlusClusterer(2, 999, distanceMeasure )
                fun cluster(map: MutableMap<Int, Double>):List<CentroidCluster<DoublePoint>>{
                    val dataPoints = map.entries.map { (index, value) ->
                        DoublePoint( doubleArrayOf(value, index.toDouble()))
                    }
                    return clusterer.cluster(dataPoints)
                }
            }
        }

        fun calcDistribution(fileName: String = "dataset.csv", columns: IntArray = intArrayOf(0,1,2,3), probabilityIntervals: IntArray = intArrayOf(10,10, 10, 10), linesToSkip: Int = 1): MutableMap<Int, FloatArray>{
            // load data
            val file = File(fileName)
            val lines = file.readLines() // Do not use this function for huge files.
            val data: MutableMap<Int, FloatArray> = mutableMapOf()
            val ret: MutableMap<Int, FloatArray> = mutableMapOf()
            for(c in columns){
                data[c] = FloatArray(lines.size-linesToSkip)

            }
            for (i in linesToSkip until lines.size){
                val values = lines[i].replace(",",".").split(";").map { token -> token.toFloat() }
                columns.forEach {
                    data[it]!![i-linesToSkip] = values[it]
                }
            }
            // count distribution
            for(pi in 0 until probabilityIntervals.size){
                ret[columns[pi]] = FloatArray(probabilityIntervals[pi])
            }
            // all data in column need to have same sign
            columns.forEach {
                var max: Float? = data[it]!!.maxOrNull()
                var min: Float? = data[it]!!.minOrNull()
                val range = abs(max!! - min!!)
                if(max <=0 && min <=max){
                    val tmp = max
                    max = abs(min)
                    min = abs(tmp)
                }
                var intCounter = 0
                for(int in 0 until probabilityIntervals[it]){
                    intCounter = 0
                    val intervalMax = range / probabilityIntervals[it] * (int+1)
                    data[it]!!.forEach { x ->
                        if(abs(x)<=intervalMax+min){
                            intCounter += 1
                        }
                    }
                    ret[it]!![int] = intCounter.toFloat()
                }
                ret[it]!!.forEachIndexed {i, x ->
                    ret[it]!![i] = x/intCounter.toFloat()
                }
            }
            plotDistribution(ret)
            return ret
        }

//        from: https://github.com/alshan/lets-plot-mini-apps/issues/2
//        alshan commented on Sep 27, 2022
//        Ho @fmvin , this must be some trick with offscreen drawing of kind. Perhaps CardLayout can help.
//        Lets-Plot itself doesn't support incremental updates at the moment so, every time the data is updated a brand new plot object must be created.

        fun plot(series: List<Pair<String, List<Double>>>): Unit {
//            // Make sure JavaFX event thread won't get killed after JFXPanel is destroyed.
//            Platform.setImplicitExit(false)

            val numSeries = series.size
            val n = series[0].second.size
            val data = mutableMapOf<String, Any>()
            data["cycle"] = List(numSeries) { (1 .. n).toList() }.flatten()
            data["Data"] = series.flatMap { (name, _) -> List(n) { name } }
            data["y"] = series.flatMap { (_, values) -> values }

            val plot = letsPlot(data){x="cycle"; color="Data"} +
                    geomLine{y = "y"}

            val rawSpec = plot.toSpec()
            val processedSpec = MonolithicCommon.processRawSpecs(rawSpec, frontendOnly = false)
            val plotSwingPanel = DefaultPlotPanelJfx(
                processedSpec = processedSpec,
                preserveAspectRatio = false,
                preferredSizeFromPlot = false,
                repaintDelay = 10,
            ) { messages ->
                for (message in messages) {
                    println("[Example App] $message")
                }
            }
            Platform.runLater {
                val swingNode = SwingNode()
                swingNode.content = plotSwingPanel
                root.center = swingNode
            }
        }

        fun plotDistribution(dis: MutableMap<Int, FloatArray>): Unit {
            val n = dis[dis.keys.first()]!!.size
            var keys = mutableListOf<String>()
            dis.keys.forEach{k ->
                keys.addAll(List(n){ k.toString() })
            }
            val data = mutableMapOf<String, Any>()
            data["interval"] = List(dis.keys.size){ (1..n).toList().map { it.toFloat()/n.toFloat() } }.flatten()
            data["p"] = dis.values.flatMap { it.asList() }.toFloatArray()
            data["l"] = keys

            val plot = letsPlot(data){ x = "interval" } + geomLine{y = "p"; color = "l"}

            val rawSpec = plot.toSpec()
            val processedSpec = MonolithicCommon.processRawSpecs(rawSpec, frontendOnly = false)
            val plotSwingPanel = DefaultPlotPanelJfx(
                processedSpec = processedSpec,
                preserveAspectRatio = false,
                preferredSizeFromPlot = false,
                repaintDelay = 10,
            ) { messages ->
                for (message in messages) {
                    println("[Example App] $message")
                }
            }
            Platform.runLater {
                val swingNode = SwingNode()
                swingNode.content = plotSwingPanel
                root.center = swingNode
            }
        }

        fun startSimulation(params: Params, misc: MiscParams, progressFun: (Double) -> Unit, returnFun: () -> Unit){
            data.clear()
            println(params.toString())
            random = MersenneTwister(100)
            thread {
                repeat(params.NC) { cycle ->
                    println("Cycle: $cycle")
                    var sumPHToS = .0 // sum of honest providers policy for its strategic clients
                    var numberHJS = 0 // number of strategic clients of honest providers
                    var sumPSToH = .0 // sum of honest providers policy for its strategic clients
                    var numberSJH = 0 // number of honest clients of strategic providers
                    var counterSSCoop = 0
                    var counterAllCoop = 0
                    // Client - provider pairs
                    val adj = Array(params.N) {IntArray(params.N)}
                    if(misc.findClients) {
                        repeat(params.N) { i -> // providers - misc.findClients
                            val numClients = random.nextInt(params.KMin, params.KMax + 1)
                            var inJ = 0
                            var rn = 0
                            while (inJ < numClients) {
                                rn = random.nextInt(0, params.N)
                                if (rn != i && adj[i][rn] != 1) {
                                    adj[i][rn] = 1
                                    if (params.sAgent!![i] == 1) { // provider is strategic
                                        if (params.sAgent!![rn] != 1) { // client is honest
                                            numberSJH += 1
                                        }
                                    } else { // provider is honest
                                        if (params.sAgent!![rn] == 1) { // client is strategic
                                            numberHJS += 1
                                        }
                                    }
                                    inJ += 1
                                }
                            }
                        }
                    }
                    else {
                        repeat(params.N) { i -> // clients - !misc.findClients
                            val numProviders = random.nextInt(params.KMin, params.KMax + 1)
                            var inJ = 0
                            var rn = 0
                            while (inJ < numProviders) {
                                rn = random.nextInt(0, params.N)
                                if (rn != i && adj[rn][i] != 1) {
                                    adj[rn][i] = 1
                                    if (params.sAgent!![i] == 1) { // client is strategic
                                        if (params.sAgent!![rn] != 1) { // provider is honest
                                            numberHJS += 1
                                        }
                                    } else { // client is honest
                                        if (params.sAgent!![rn] == 1) { // provider is strategic
                                            numberSJH += 1
                                        }
                                    }
                                    inJ += 1
                                }
                            }
                        }
                    }
                    // Agents policy
                    var agentsR: MutableMap<Int, Double> = mutableMapOf()
                    var symmetricAH = randExpoD(params.ExpoA)
                    var symmetricAS = randExpoD(params.ExpoA)
                    var symmetricGH = if(misc.useCalcDist) randCalcDist() else randExpoD(params.ExpoG)
                    var symmetricGS = if(misc.useCalcDist) randCalcDist() else randExpoD(params.ExpoG)
                    repeat(params.N) {
                        val clients = adj[it]
                        val vProv = if (cycle == 0) params.V0 else data[cycle-1].V[it] // Vi
                        val numClients = clients.count{x->x == 1}
                        var meanPolicyR = .0
                        repeat(params.N){j->
                            if(adj[it][j]==1){
                                val vRepo = if (cycle == 0) params.V0 else data[cycle-1].V[j] // Vj
                                var L = hStep(vRepo, params.X) // L(Vj,x)
                                var p = if(params.sAgent!![it]==1) sBiasP(params.Y, L) else L // pij
                                if(params.sAgent!![it]==1 && params.sAgent!![j]==1) p = 1.0 // s-agents cooperation
                                val policyP = if (misc.symmetricalModel)
                                    if (params.sAgent!![it]==1) providerPolicy(symmetricAS, p) else providerPolicy(symmetricAH, p)
                                    else providerPolicy(randExpoD(params.ExpoA), p) // Pij
                                L = hStep(vProv, params.X) // L(Vi,x)
                                var r = if(params.sAgent!![j]==1) sBiasR(params.Z, L) else L // rij
                                if(params.sAgent!![it]==1 && params.sAgent!![j]==1) r = 1.0 // s-agents cooperation
                                val policyR = if (misc.symmetricalModel)
                                    if (params.sAgent!![it]==1) providerPolicy(symmetricGS, p) else providerPolicy(symmetricGH, p)
                                    else reporterPolicy( if(misc.useCalcDist) randCalcDist() else randExpoD(params.ExpoG) , policyP, r) // Rij
                                meanPolicyR += policyR*vProv
                                if(params.sAgent!![it]==1 && params.sAgent!![j]==1) counterSSCoop += 1 // debug
                                counterAllCoop += 1 // debug
                                if(params.sAgent!![it]==1){ // provider is strategic
                                    if(params.sAgent!![j]!=1){ // client is honest
                                        sumPSToH += policyP
                                    }
                                }
                                else{ // provider is honest
                                    if(params.sAgent!![j]==1){ // client is strategic
                                        sumPHToS += policyP
                                    }
                                }
                            }
                        }
                        meanPolicyR /= numClients // Provider reputation
                        agentsR[it] = meanPolicyR
                    }


                    // Reputation aggregation
                    // Clustering K-Means
                    val clusters = cluster(agentsR)

                    var meanRHigherSet = .0
                    var meanRLowerSet = .0
                    for(point in clusters[0].points){
                        meanRHigherSet += point.point[0]
                    }
                    meanRHigherSet /= clusters[0].points.size

                    for(point in clusters[1].points){
                        meanRLowerSet += point.point[0]
                    }
                    meanRLowerSet /= clusters[1].points.size

                    var swap = false
                    if(meanRLowerSet>meanRHigherSet){
                        swap = true
                        val tmp = meanRHigherSet
                        meanRHigherSet = meanRLowerSet
                        meanRLowerSet = tmp
                    }

                    println("meanRHigherSet: ${String.format("%.2f",meanRHigherSet)}, meanRLowerSet: ${String.format("%.2f",meanRLowerSet)}")

                    // normalization (0,higher) -> (0,1)
                    meanRLowerSet /= meanRHigherSet
                    meanRHigherSet /= meanRHigherSet
                    // Trust measure
                    var V = DoubleArray(params.N)
                    for(point in clusters[0].points){
                        V[point.point[1].toInt()] = if(swap) meanRLowerSet else meanRHigherSet
                    }
                    for(point in clusters[1].points){
                        V[point.point[1].toInt()] = if(swap) meanRHigherSet else meanRLowerSet
                    }

                    var meanVs = 0.0
                    var meanVh = 0.0
                    repeat(params.N){
                        if(params.sAgent!![it]==1){
                            meanVs += V[it]
                        }
                        else{
                            meanVh += V[it]
                        }
                    }
                    meanVh /= params.N-params.S
                    meanVs /= params.S

                    val netOutflow = sumPHToS/numberHJS - sumPSToH/numberSJH
                    println("counterSSCoop: $counterSSCoop/$counterAllCoop, netOutflow: ${String.format("%.2f",netOutflow)}" +
                            ", sumPHToS: ${String.format("%.2f",sumPHToS)}, numberHJS: $numberHJS" +
                            ", sumPSToH: ${String.format("%.2f",sumPSToH)}, numberSJH: $numberSJH")

                    data += Cycle(V, meanVs, meanVh, netOutflow, BooleanArray(params.N), counterSSCoop, counterAllCoop)
                    println("meanRHigherSet: ${String.format("%.2f",meanRHigherSet)}, meanRLowerSet: ${String.format("%.2f",meanRLowerSet)}")
                    println("meanVs: ${String.format("%.2f",meanVs)}, meanVh: ${String.format("%.2f",meanVh)}")
                    Thread.sleep(10)
                    progressFun((cycle+1).toDouble()/params.NC*100)
                }

                val date = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
                val fileWriter = FileWriter("./out_$date.csv")
                val bufferedWriter = BufferedWriter(fileWriter)
                bufferedWriter.write(params.toString())
                bufferedWriter.newLine()
                bufferedWriter.write("meanVs;meanVh;netOutflow;")
                bufferedWriter.newLine()
                for(cycle in data){
                    bufferedWriter.write("${cycle.meanVs};${cycle.meanVh};${cycle.netOutflow};")
                    bufferedWriter.newLine()
                }
                bufferedWriter.close()

                val series = listOf(
                    Pair("meanVh", data.map {cycle->cycle.meanVh}),
                    Pair("meanVs", data.map {cycle->cycle.meanVs}),
                    Pair("netOutflow", data.map {cycle->cycle.netOutflow})
                )
                plot(series)
                returnFun()
            }
        }
    }

}

private fun MersenneTwister.nextInt(from: Int, until: Int): Int {
    require(from < until){"Invalid range: a ($from) is greater than b ($until)"}
    return (this.nextDouble()*(until-from-1) + from).roundToInt()
}

fun main() {
    Locale.setDefault(Locale.US)
    Application.launch(AgentsApp::class.java)
}