package com.tangem.common.ui.charts.downsample

import kotlin.math.max

/**
 * =========================================================

 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================================================
 *
 * Downsamples the given data points to the desired number of buckets (points + 2).
 *
* [REDACTED_AUTHOR]
 */
object LTThreeBuckets {

    fun downsample(x: List<Double>, y: List<Double>, desiredBuckets: Int): Result {
        require(x.size == y.size) { "X and Y must have the same size" }
        require(desiredBuckets > 0) { "Desired buckets must be greater than 0" }

        val points = x.zip(y).mapIndexed { index, (x, y) -> Point(index, x, y) }
        val results = mutableListOf<Point>()

        points.onPassBucketize(desiredBuckets)
            .sliding(size = 3, step = 1)
            .map { buckets -> Triangle.of(buckets) }
            .fastForEach { triangle ->
                if (results.isEmpty()) {
                    results.add(triangle.getFirst())
                }

                results.add(triangle.getResult())

                if (results.size == desiredBuckets + 1) {
                    results.add(triangle.getLast())
                }
            }

        val xRes = ArrayList<Double>(points.size)
        val yRes = ArrayList<Double>(points.size)
        val indexesRes = ArrayList<Int>(points.size)

        results.fastForEach {
            xRes.add(it.x)
            yRes.add(it.y)
            indexesRes.add(it.originalIndex!!)
        }

        return Result(
            originalIndexes = indexesRes,
            x = xRes,
            y = yRes,
        )
    }

    data class Result(
        val originalIndexes: List<Int>,
        val x: List<Double>,
        val y: List<Double>,
    )
}

private fun List<Point>.onPassBucketize(desiredBucketsCount: Int): List<Bucket> {
    val middleSize = size - 2
    val bucketSize = middleSize / desiredBucketsCount
    val remainingElements = middleSize % desiredBucketsCount

    require(bucketSize != 0) {
        "Can't produce $desiredBucketsCount buckets from an input series of ${middleSize + 2} elements"
    }

    val buckets = mutableListOf<Bucket>()

    // Add first point as the only point in the first bucket
    buckets.add(Bucket.of(this[0]))

    var rest = this.subList(1, this.lastIndex)

    // Add middle buckets.
    // When inputSize is not a multiple of desiredBuckets,
    // remaining elements are equally distributed on the first buckets.
    while (buckets.size < desiredBucketsCount + 1) {
        val size = if (buckets.size <= remainingElements) bucketSize + 1 else bucketSize
        buckets.add(Bucket.of(rest.subList(0, size)))
        rest = rest.subList(size, rest.size)
    }

    // Add last point as the only point in the last bucket
    buckets.add(Bucket.of(this.last()))

    return buckets
}

private fun List<Bucket>.sliding(size: Int, step: Int): List<List<Bucket>> {
    val window = max(size, step)
    val buffer = ArrayDeque<Bucket>()
    var totalIn = 0

    val lists = mutableListOf<List<Bucket>>()

    fastForEach { p ->
        buffer.add(p)
        ++totalIn
        if (buffer.size == window) {
            val batch = buffer.take(size)
            lists.add(batch)

            repeat(step) {
                buffer.removeFirst()
            }
        }
    }

    if (buffer.isNotEmpty()) {
        val totalOut = max(0, (totalIn + step - size - 1) / step) + 1
        if (totalOut > lists.size) {
            val batch = buffer.take(size)
            lists.add(batch)
        }
    }

    return lists
}

private data class Point(
    val originalIndex: Int? = null,
    val x: Double,
    val y: Double,
)

private data class Bucket(
    val data: List<Point>,
    val center: Point,
    val result: Point,
    val first: Point,
    val last: Point,
) {
    companion object {
        private fun centerBetweenPoints(a: Point, b: Point): Point {
            val vector = Point(
                x = b.x - a.x,
                y = b.y - a.y,
            )
            val halfVector = Point(
                x = vector.x / 2,
                y = vector.y / 2,
            )

            return Point(
                x = a.x + halfVector.x,
                y = a.y + halfVector.y,
            )
        }

        fun of(points: List<Point>): Bucket {
            val first = points.first()
            val last = points.last()

            return Bucket(
                data = points,
                center = centerBetweenPoints(first, last),
                result = first,
                first = first,
                last = last,
            )
        }

        fun of(point: Point): Bucket {
            return Bucket(
                data = listOf(point),
                center = point,
                result = point,
                first = point,
                last = point,
            )
        }
    }
}

private data class Triangle(
    val left: Bucket,
    val center: Bucket,
    val right: Bucket,
) {
    fun getResult(): Point {
        return center.data.map { Area.ofTriangle(left.result, it, right.center) }
            .maxByOrNull { it.value }
            ?.generator
            ?: error("Can't obtain max area triangle")
    }

    fun getFirst(): Point {
        return left.first
    }

    fun getLast(): Point {
        return right.last
    }

    companion object {
        fun of(buckets: List<Bucket>): Triangle {
            return Triangle(
                left = buckets[0],
                center = buckets[1],
                right = buckets[2],
            )
        }
    }
}

private data class Area(
    val generator: Point,
    val value: Double,
) {
    companion object {
        fun ofTriangle(a: Point, b: Point, c: Point): Area {
            val addends = listOf(
                a.x * (b.y - c.y),
                b.x * (c.y - a.y),
                c.x * (a.y - b.y),
            )
            val sum = addends.sum()
            val value = kotlin.math.abs(sum / 2)

            return Area(b, value)
        }
    }
}

inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}
