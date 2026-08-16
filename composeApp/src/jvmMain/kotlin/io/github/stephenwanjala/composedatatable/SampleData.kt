package io.github.stephenwanjala.composedatatable

import java.util.*
import kotlin.random.Random

/**
 * Wide row type used by the samples that need many columns.
 */
data class LargeDataSetItem(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val email: String,
    val city: String,
    val country: String,
    val occupation: String,
    val salary: Double,
    val startDate: String,
    val projectStatus: String,
    val hoursWorked: Int,
    val department: String,
    val notes: String,
    val isActive: Boolean,
    val rating: Double,
    val version: String,
    val licenseKey: String,
    val lastLogin: String,
    val ipAddress: String,
    val macAddress: String,
    val phoneNumber: String,
    val zipCode: String,
    val streetAddress: String,
    val buildingNumber: String,
    val floorNumber: Int,
    val officeNumber: Int,
    val managerName: String,
    val teamLead: String,
    val reviewScore: Int,
) {
    val fullName: String
        get() = "$firstName $lastName"

    companion object {
        private val firstNames =
            listOf("Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy")
        private val lastNames = listOf(
            "Smith", "Johnson", "Williams", "Brown", "Jones",
            "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"
        )
        private val cities = listOf(
            "New York", "Los Angeles", "Chicago", "Houston",
            "Phoenix", "Philadelphia", "San Antonio", "San Diego"
        )
        private val countries =
            listOf("USA", "Canada", "Mexico", "Brazil", "UK", "Germany", "France", "Spain", "Italy", "Japan")
        private val occupations = listOf(
            "Engineer", "Doctor", "Teacher", "Artist",
            "Programmer", "Analyst", "Manager", "Consultant", "Designer"
        )
        private val projectStatuses = listOf("Completed", "In Progress", "Pending", "On Hold", "Cancelled")
        private val departments =
            listOf("IT", "HR", "Finance", "Marketing", "Sales", "Operations", "Research", "Development")

        fun generateRandom(id: Int, random: Random): LargeDataSetItem {
            val firstName = firstNames.random(random)
            val lastName = lastNames.random(random)
            val age = random.nextInt(22, 65)
            val email =
                "${firstName.lowercase(Locale.getDefault())}.${lastName.lowercase(Locale.getDefault())}@example.com"
            val city = cities.random(random)
            val country = countries.random(random)
            val occupation = occupations.random(random)
            val salary = random.nextDouble(40000.0, 120000.0).round(2)
            val startDate = "20${random.nextInt(10, 24)}-${random.nextInt(1, 12).toString().padStart(2, '0')}-${
                random.nextInt(1, 28).toString().padStart(2, '0')
            }"
            val projectStatus = projectStatuses.random(random)
            val hoursWorked = random.nextInt(100, 2000)
            val department = departments.random(random)
            val notes =
                "This is a random note for $firstName $lastName generated for testing purposes. It can be quite long."
            val isActive = random.nextBoolean()
            val rating = random.nextDouble(1.0, 5.0).round(1)
            val version = "${random.nextInt(1, 5)}.${random.nextInt(0, 10)}.${random.nextInt(0, 10)}"
            val licenseKey =
                (1..5).map { (0..9).random(random) }.joinToString("") + "-" + (1..5).map { (0..9).random(random) }
                    .joinToString("")
            val lastLogin = "2024-03-${random.nextInt(1, 28).toString().padStart(2, '0')} ${
                random.nextInt(0, 23).toString().padStart(2, '0')
            }:${random.nextInt(0, 59).toString().padStart(2, '0')}"
            val ipAddress = "${random.nextInt(0, 255)}.${random.nextInt(0, 255)}.${random.nextInt(0, 255)}.${
                random.nextInt(0, 255)
            }"
            val macAddress = (1..6).joinToString(":") { String.format("%02X", random.nextInt(256)) }
            val phoneNumber = "+1-${random.nextInt(200, 999)}-${random.nextInt(100, 999)}-${random.nextInt(1000, 9999)}"
            val zipCode = "${random.nextInt(10000, 99999)}"
            val streetAddress = "${
                random.nextInt(100, 999)
            } ${firstNames.random(random)} ${if (random.nextBoolean()) "Street" else "Avenue"}"
            val buildingNumber = "${random.nextInt(1, 20)}"
            val floorNumber = random.nextInt(1, 30)
            val officeNumber = random.nextInt(101, 500)
            val managerName = firstNames.random(random) + " " + lastNames.random(random)
            val teamLead = firstNames.random(random) + " " + lastNames.random(random)
            val reviewScore = random.nextInt(1, 10)

            return LargeDataSetItem(
                id, firstName, lastName, age, email, city, country, occupation, salary, startDate,
                projectStatus, hoursWorked, department, notes, isActive, rating, version, licenseKey,
                lastLogin, ipAddress, macAddress, phoneNumber, zipCode, streetAddress, buildingNumber,
                floorNumber, officeNumber, managerName, teamLead, reviewScore
            )
        }

        fun Double.round(decimals: Int): Double {
            var multiplier = 1.0
            repeat(decimals) { multiplier *= 10 }
            return (this * multiplier).toLong() / multiplier
        }
    }
}

/**
 * Narrow row type for the focused samples, where 30 columns would only get in the way.
 */
data class Employee(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val role: String,
    val salary: Double,
    val active: Boolean,
)

val sampleEmployees: List<Employee> = run {
    val random = Random(7)
    val names = listOf(
        "Alice Smith", "Bob Johnson", "Charlie Brown", "Diana Prince", "Eve Adams",
        "Frank Castle", "Grace Hopper", "Heidi Klum", "Ivan Petrov", "Judy Garland",
        "Karl Weber", "Lena Fischer", "Mia Novak", "Noah Kaur", "Olga Ivanova",
        "Pavel Novy", "Quinn Adeyemi", "Rosa Lima", "Sam Okafor", "Tara Singh",
    )
    val departments = listOf("Finance", "IT", "Operations", "Sales")
    val roles = listOf("Analyst", "Engineer", "Manager", "Coordinator")

    names.mapIndexed { index, name ->
        Employee(
            id = index + 1,
            name = name,
            email = name.lowercase(Locale.getDefault()).replace(" ", ".") + "@example.com",
            phone = "+1-${random.nextInt(200, 999)}-${random.nextInt(1000, 9999)}",
            department = departments[index % departments.size],
            role = roles[index % roles.size],
            salary = random.nextInt(45_000, 130_000).toDouble(),
            active = random.nextInt(10) > 2,
        )
    }
}

/**
 * Stands in for a repository backed by a database, so the server-side sample has something to
 * page and sort against without holding every row in memory.
 */
object EmployeeRepository {
    private val all: List<Employee> = run {
        val random = Random(99)
        val departments = listOf("Finance", "IT", "Operations", "Sales", "Legal", "Support")
        val roles = listOf("Analyst", "Engineer", "Manager", "Coordinator", "Director")
        (1..5_000).map { id ->
            Employee(
                id = id,
                name = "Employee $id",
                email = "employee$id@example.com",
                phone = "+1-${random.nextInt(200, 999)}-${random.nextInt(1000, 9999)}",
                department = departments.random(random),
                role = roles.random(random),
                salary = random.nextInt(40_000, 150_000).toDouble(),
                active = random.nextInt(10) > 1,
            )
        }
    }

    val total: Int get() = all.size

    /** Mimics `ORDER BY ... LIMIT ... OFFSET ...`. */
    fun page(offset: Int, limit: Int, sortKey: String, ascending: Boolean): List<Employee> {
        val comparator: Comparator<Employee> = when (sortKey) {
            "name" -> compareBy { it.id }          // "Employee 12" sorts naturally by id here
            "department" -> compareBy { it.department }
            "role" -> compareBy { it.role }
            "salary" -> compareBy { it.salary }
            else -> compareBy { it.id }
        }
        return all.sortedWith(if (ascending) comparator else comparator.reversed())
            .drop(offset)
            .take(limit)
    }
}
