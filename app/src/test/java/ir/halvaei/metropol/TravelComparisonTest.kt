package ir.halvaei.metropol

import ir.halvaei.metropol.data.source.MetroNetworkParser
import ir.halvaei.metropol.domain.model.LatLng
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.repository.CarDirectionsRepository
import ir.halvaei.metropol.domain.routing.RoutePlanner
import ir.halvaei.metropol.domain.usecase.EstimateCarMinutesUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class TravelComparisonTest {

    private val planner by lazy {
        RoutePlanner(MetroNetworkParser.parse(File("src/main/assets/metro_network.json").readText()))
    }
    private val tajrishToAzadegan by lazy {
        planner.plan("tajrish", "azadegan", RoutePreference.FASTEST, 12 * 60)!!
    }

    private class FakeCarDirections(private val result: Result<Double>) : CarDirectionsRepository {
        var from: LatLng? = null
        var to: LatLng? = null
        override suspend fun drivingMinutes(from: LatLng, to: LatLng): Result<Double> {
            this.from = from
            this.to = to
            return result
        }
    }

    @Test
    fun `car minutes are rounded up between the end stations`() = runBlocking {
        val repo = FakeCarDirections(Result.success(41.2))
        val minutes = EstimateCarMinutesUseCase(repo)(tajrishToAzadegan).getOrThrow()
        assertEquals(42, minutes)
        assertEquals(tajrishToAzadegan.origin.location, repo.from)
        assertEquals(tajrishToAzadegan.destination.location, repo.to)
    }

    @Test
    fun `car failure is passed through`() = runBlocking {
        val repo = FakeCarDirections(Result.failure(IOException("offline")))
        assertTrue(EstimateCarMinutesUseCase(repo)(tajrishToAzadegan).isFailure)
    }
}
