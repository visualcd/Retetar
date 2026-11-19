package com.ro.aminvatatro.domain.usecase

import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.data.importexport.RECIPE_EXPORT_SCHEMA_VERSION
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.usecase.importexport.DuplicatePolicy
import com.ro.aminvatatro.domain.usecase.importexport.ExportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipesResult
import com.ro.aminvatatro.domain.usecase.importexport.ImportValidationError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ImportExportUseCasesTest {

    @Test
    fun exportAndImportRoundTripPreservesAllRecipeDataAndSupportsSubsetExport() = runTest {
        val sourceRepo = FakeRecipeRepository()
        val targetRepo = FakeRecipeRepository()
        val clock = FakeClock(currentMillis = 123_456_789L)
        val ids = SequenceIdGenerator("imp")

        val r1 = sampleRecipe(
            id = "rec-1",
            title = "Clătite cu brânză",
            baseServings = 4,
            prepMinutes = 15,
            cookMinutes = 25,
            tags = listOf(Tag("t1", "Desert"), Tag("t2", "Rapid")),
            isFavorite = true,
            createdAt = 10_000L,
            updatedAt = 20_000L,
        )
        val r2 = sampleRecipe(
            id = "rec-2",
            title = "Supă cremă de legume",
            baseServings = 6,
            ingredients = listOf(
                Ingredient("i-supa-1", 0, "Morcovi", 300.0, MeasurementUnit.G, Aisle.PRODUCE),
                Ingredient("i-supa-2", 1, "Apă", 1.5, MeasurementUnit.L, Aisle.OTHER),
            ),
            steps = listOf(Step("s-supa-1", 0, "Fierbe și pasează.")),
            tags = listOf(Tag("t3", "Supă")),
            createdAt = 11_000L,
            updatedAt = 21_000L,
        )
        sourceRepo.saveRecipes(listOf(r1, r2))

        val exportRecipes = ExportRecipes(sourceRepo, clock)
        val importRecipes = ImportRecipes(targetRepo, clock, ids)

        val subsetExport = exportRecipes(recipeIds = setOf("rec-1"))
        assertEquals(1, subsetExport.recipeCount)
        assertEquals(RECIPE_EXPORT_SCHEMA_VERSION, subsetExport.schemaVersion)
        assertTrue(subsetExport.json.contains("\"schemaVersion\": 1"))
        assertTrue(subsetExport.json.contains("\"exportedAt\": 123456789"))

        val fullExport = exportRecipes()
        assertEquals(2, fullExport.recipeCount)

        val imported = assertIs<ImportRecipesResult.Success>(
            importRecipes(fullExport.json, DuplicatePolicy.SKIP),
        )
        assertEquals(2, imported.importedCount)
        assertEquals(0, imported.skippedCount)
        assertEquals(0, imported.duplicatedCount)
        assertEquals(1, targetRepo.saveRecipesCallCount, "All recipes must be written in a single transaction")

        val restored1 = targetRepo.getRecipeById("rec-1")!!
        assertEquals(r1.title, restored1.title)
        assertEquals(r1.baseServings, restored1.baseServings)
        assertEquals(r1.prepMinutes, restored1.prepMinutes)
        assertEquals(r1.cookMinutes, restored1.cookMinutes)
        assertEquals(r1.isFavorite, restored1.isFavorite)
        assertEquals(r1.notes, restored1.notes)
        assertEquals(r1.ingredients.map { it.name to it.unit }, restored1.ingredients.map { it.name to it.unit })
        assertEquals(r1.steps.map { it.text }, restored1.steps.map { it.text })
        assertEquals(r1.tags.map { it.name }, restored1.tags.map { it.name })
    }

    @Test
    fun importReturnsTypedErrorsOnCorruptJsonAndUnknownSchemaVersion() = runTest {
        val repo = FakeRecipeRepository()
        val importRecipes = ImportRecipes(repo, FakeClock(), SequenceIdGenerator())

        assertIs<ImportRecipesResult.InvalidJson>(importRecipes(""))
        assertIs<ImportRecipesResult.InvalidJson>(importRecipes("{not valid json"))
        assertIs<ImportRecipesResult.InvalidJson>(importRecipes("[]"))
        assertIs<ImportRecipesResult.InvalidJson>(importRecipes("""{"exportedAt": 1, "recipes": []}"""))
        assertIs<ImportRecipesResult.InvalidJson>(importRecipes("""{"schemaVersion": "1", "exportedAt": 1, "recipes": []}"""))
        assertIs<ImportRecipesResult.InvalidJson>(importRecipes("""{"schemaVersion": 1, "exportedAt": 1}"""))

        val futureVersion = assertIs<ImportRecipesResult.UnsupportedSchemaVersion>(
            importRecipes("""{"schemaVersion": 2, "futureField": true}"""),
        )
        assertEquals(2, futureVersion.found)
        assertEquals(RECIPE_EXPORT_SCHEMA_VERSION, futureVersion.supported)

        val zeroVersion = assertIs<ImportRecipesResult.UnsupportedSchemaVersion>(
            importRecipes("""{"schemaVersion": 0, "exportedAt": 1, "recipes": []}"""),
        )
        assertEquals(0, zeroVersion.found)
        assertEquals(0, repo.saveRecipesCallCount)
    }

    @Test
    fun importValidatesEntirePayloadAtomicallyBeforeWritingAnything() = runTest {
        val repo = FakeRecipeRepository()
        val importRecipes = ImportRecipes(repo, FakeClock(), SequenceIdGenerator())

        val payloadWithSecondRecipeInvalid = """
            {
              "schemaVersion": 1,
              "exportedAt": 1000,
              "recipes": [
                {
                  "id": "valid-1",
                  "title": "Rețetă validă",
                  "baseServings": 2,
                  "ingredients": [{"name": "Apă", "quantity": 100.0, "unit": "ML", "aisle": "OTHER"}],
                  "steps": [{"text": "Fierbe."}]
                },
                {
                  "id": "bad-2",
                  "title": "",
                  "baseServings": 0,
                  "ingredients": [{"name": "X", "quantity": 10.0, "unit": "GALLON", "aisle": "UNKNOWN_AISLE"}],
                  "steps": []
                }
              ]
            }
        """.trimIndent()

        val result = assertIs<ImportRecipesResult.ValidationFailed>(importRecipes(payloadWithSecondRecipeInvalid))
        assertTrue(result.errors.any { it is ImportValidationError.UnknownUnit && it.rawValue == "GALLON" })
        assertTrue(result.errors.any { it is ImportValidationError.UnknownAisle && it.rawValue == "UNKNOWN_AISLE" })
        assertTrue(repo.getAllRecipes().isEmpty(), "Valid first recipe must NOT be persisted when batch fails")
        assertEquals(0, repo.saveRecipesCallCount)

        val payloadWithDomainError = """
            {
              "schemaVersion": 1,
              "exportedAt": 1000,
              "recipes": [
                {
                  "id": "bad-domain",
                  "title": "Valid title",
                  "baseServings": 2,
                  "ingredients": [{"name": "Sare", "quantity": 5.0, "unit": "TO_TASTE", "aisle": "SPICES"}],
                  "steps": [{"text": "Amestecă."}]
                }
              ]
            }
        """.trimIndent()

        val domainResult = assertIs<ImportRecipesResult.ValidationFailed>(importRecipes(payloadWithDomainError))
        assertTrue(domainResult.errors.single() is ImportValidationError.RecipeInvalid)
        assertEquals(0, repo.saveRecipesCallCount)
    }

    @Test
    fun importAppliesSkipAndKeepBothDuplicatePoliciesByIdAndNormalizedTitle() = runTest {
        val repo = FakeRecipeRepository()
        val clock = FakeClock(currentMillis = 50_000L)
        val ids = SequenceIdGenerator("dup")

        val existing = sampleRecipe(id = "r-existing", title = "Clătite cu brânză")
        repo.saveRecipe(existing)

        val importRecipes = ImportRecipes(repo, clock, ids)

        val incomingJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 40000,
              "recipes": [
                {
                  "id": "r-existing",
                  "title": "Titlu diferit dar același ID",
                  "baseServings": 2,
                  "ingredients": [{"id": "ing-1", "name": "Ouă", "quantity": 2.0, "unit": "PIECE", "aisle": "DAIRY"}],
                  "steps": [{"id": "step-1", "text": "Gătește."}]
                },
                {
                  "id": "r-different-id",
                  "title": "clatite   cu BRANZA",
                  "baseServings": 4,
                  "ingredients": [{"id": "ing-2", "name": "Făină", "quantity": 200.0, "unit": "G", "aisle": "PANTRY"}],
                  "steps": [{"id": "step-2", "text": "Coace."}]
                },
                {
                  "id": "r-brand-new",
                  "title": "Salată grecească",
                  "baseServings": 2,
                  "ingredients": [{"id": "ing-3", "name": "Roșii", "quantity": 2.0, "unit": "PIECE", "aisle": "PRODUCE"}],
                  "steps": [{"id": "step-3", "text": "Taie legumele."}]
                }
              ]
            }
        """.trimIndent()

        // SKIP policy: skips both duplicates (by id and by normalized title), imports only the brand new recipe
        val skipResult = assertIs<ImportRecipesResult.Success>(
            importRecipes(incomingJson, DuplicatePolicy.SKIP),
        )
        assertEquals(1, skipResult.importedCount)
        assertEquals(2, skipResult.skippedCount)
        assertEquals(0, skipResult.duplicatedCount)
        assertEquals(2, repo.getAllRecipes().size)
        assertEquals("Clătite cu brânză", repo.getRecipeById("r-existing")?.title)

        // KEEP_BOTH policy: imports all 3 as copies with fresh unique IDs, keeping originals intact
        val keepBothResult = assertIs<ImportRecipesResult.Success>(
            importRecipes(incomingJson, DuplicatePolicy.KEEP_BOTH),
        )
        assertEquals(3, keepBothResult.importedCount)
        assertEquals(0, keepBothResult.skippedCount)
        assertEquals(3, keepBothResult.duplicatedCount)

        val allAfter = repo.getAllRecipes()
        assertEquals(5, allAfter.size)
        assertEquals(5, allAfter.map { it.id }.toSet().size)
        val copiesOfExisting = allAfter.filter { it.title == "Titlu diferit dar același ID" }
        assertEquals(1, copiesOfExisting.size)
        assertNotEquals("r-existing", copiesOfExisting.single().id)
        assertNotEquals("ing-1", copiesOfExisting.single().ingredients.single().id)
    }
}
