package de.rr.universelauncher.physics

import androidx.compose.ui.graphics.Color
import kotlin.math.*

/**
 * Represents a planet in the orbital system
 */
data class Planet(
    val name: String,
    val mass: Double, // in solar masses
    val radius: Float, // visual radius in pixels
    val color: Color,
    val semiMajorAxis: Double, // in AU (Astronomical Units)
    val eccentricity: Double, // 0 = circular, 1 = parabolic
    val inclination: Double, // orbital inclination in degrees
    val argumentOfPeriapsis: Double, // argument of periapsis in degrees
    val meanAnomaly: Double, // current position in orbit (0-360 degrees)
    val orbitalPeriod: Double // in days
)

/**
 * Represents the central star
 */
data class Star(
    val name: String,
    val mass: Double, // in solar masses
    val radius: Float, // visual radius in pixels
    val color: Color
)

/**
 * Complete orbital system with star and planets
 */
data class OrbitalSystem(
    val star: Star,
    val planets: List<Planet>
)

/**
 * Physics calculations for orbital mechanics
 */
object OrbitalPhysics {
    
    // Constants
    private const val G = 6.67430e-11 // Gravitational constant
    private const val AU_TO_METERS = 1.496e11 // Astronomical Unit to meters
    private const val SOLAR_MASS_TO_KG = 1.989e30 // Solar mass to kilograms
    private const val DAYS_TO_SECONDS = 86400.0 // Days to seconds
    
    /**
     * Calculate the position of a planet at a given time using Kepler's equation
     */
    fun calculatePlanetPosition(
        planet: Planet,
        timeDays: Double,
        starMass: Double
    ): Pair<Double, Double> {
        // Calculate mean anomaly at current time
        val meanAnomaly = (planet.meanAnomaly + (360.0 * timeDays / planet.orbitalPeriod)) % 360.0
        
        // Convert to radians
        val M = Math.toRadians(meanAnomaly)
        val e = planet.eccentricity
        
        // Solve Kepler's equation: M = E - e*sin(E)
        val E = solveKeplersEquation(M, e)
        
        // Calculate true anomaly
        val trueAnomaly = 2.0 * atan(sqrt((1.0 + e) / (1.0 - e)) * tan(E / 2.0))
        
        // Calculate distance from star
        val a = planet.semiMajorAxis * AU_TO_METERS
        val r = a * (1.0 - e * e) / (1.0 + e * cos(trueAnomaly))
        
        // Convert to screen coordinates (simplified - no 3D rotation for now)
        val x = r * cos(trueAnomaly) / AU_TO_METERS
        val y = r * sin(trueAnomaly) / AU_TO_METERS
        
        return Pair(x, y)
    }
    
    /**
     * Solve Kepler's equation using Newton-Raphson method
     */
    private fun solveKeplersEquation(M: Double, e: Double, maxIterations: Int = 10): Double {
        var E = M // Initial guess
        for (i in 0 until maxIterations) {
            val f = E - e * sin(E) - M
            val fPrime = 1.0 - e * cos(E)
            if (abs(fPrime) < 1e-10) break
            E = E - f / fPrime
        }
        return E
    }
    
    /**
     * Create a sample solar system with realistic parameters
     */
    fun createSampleSolarSystem(): OrbitalSystem {
        val sun = Star(
            name = "Sun",
            mass = 1.0,
            radius = 20f,
            color = Color(0xFFFFD700) // Gold
        )
        
        val planets = listOf(
            Planet(
                name = "Mercury",
                mass = 0.000055,
                radius = 4f,
                color = Color(0xFF8C7853), // Brown
                semiMajorAxis = 0.387,
                eccentricity = 0.205,
                inclination = 7.0,
                argumentOfPeriapsis = 29.0,
                meanAnomaly = 0.0,
                orbitalPeriod = 88.0
            ),
            Planet(
                name = "Venus",
                mass = 0.000815,
                radius = 6f,
                color = Color(0xFFFFC649), // Yellow
                semiMajorAxis = 0.723,
                eccentricity = 0.007,
                inclination = 3.4,
                argumentOfPeriapsis = 55.0,
                meanAnomaly = 0.0,
                orbitalPeriod = 225.0
            ),
            Planet(
                name = "Earth",
                mass = 0.000003,
                radius = 6f,
                color = Color(0xFF6B93D6), // Blue
                semiMajorAxis = 1.0,
                eccentricity = 0.017,
                inclination = 0.0,
                argumentOfPeriapsis = 114.0,
                meanAnomaly = 0.0,
                orbitalPeriod = 365.0
            ),
            Planet(
                name = "Mars",
                mass = 0.0000003,
                radius = 5f,
                color = Color(0xFFCD5C5C), // Red
                semiMajorAxis = 1.524,
                eccentricity = 0.094,
                inclination = 1.9,
                argumentOfPeriapsis = 286.0,
                meanAnomaly = 0.0,
                orbitalPeriod = 687.0
            ),
            Planet(
                name = "Jupiter",
                mass = 0.00095,
                radius = 12f,
                color = Color(0xFFD8CA9D), // Tan
                semiMajorAxis = 5.203,
                eccentricity = 0.049,
                inclination = 1.3,
                argumentOfPeriapsis = 275.0,
                meanAnomaly = 0.0,
                orbitalPeriod = 4333.0
            ),
            Planet(
                name = "Saturn",
                mass = 0.0003,
                radius = 10f,
                color = Color(0xFFFAD5A5), // Light tan
                semiMajorAxis = 9.537,
                eccentricity = 0.057,
                inclination = 2.5,
                argumentOfPeriapsis = 336.0,
                meanAnomaly = 0.0,
                orbitalPeriod = 10759.0
            ),
            Planet(
                name = "Uranus",
                mass = 0.000045,
                radius = 8f,
                color = Color(0xFF4FD0E7), // Cyan
                semiMajorAxis = 19.191,
                eccentricity = 0.046,
                inclination = 0.8,
                argumentOfPeriapsis = 96.0,
                meanAnomaly = 0.0,
                orbitalPeriod = 30687.0
            ),
            Planet(
                name = "Neptune",
                mass = 0.000054,
                radius = 8f,
                color = Color(0xFF4B70DD), // Blue
                semiMajorAxis = 30.069,
                eccentricity = 0.009,
                inclination = 1.8,
                argumentOfPeriapsis = 276.0,
                meanAnomaly = 0.0,
                orbitalPeriod = 60190.0
            )
        )
        
        return OrbitalSystem(sun, planets)
    }
}
