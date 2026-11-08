package nyc.arash.homescreen.apps

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryTest {
    @Test fun wellKnownAppsGoToTheirGroup() {
        assertEquals(AppCategory.STREAMING, AppCategory.of("com.netflix.ninja", -1))
        assertEquals(AppCategory.STREAMING, AppCategory.of("com.amazon.firebat", -1))
        assertEquals(AppCategory.LIVE, AppCategory.of("com.amazon.firetv.youtube.tv", -1))
        assertEquals(AppCategory.LIVE, AppCategory.of(AppRepository.LIVE_TV_ID, -1))
        assertEquals(AppCategory.KIDS, AppCategory.of("org.pbskids.video", ApplicationInfo.CATEGORY_VIDEO))
        assertEquals(AppCategory.MUSIC, AppCategory.of("com.amazon.bueller.music", -1))
    }

    @Test fun otherAppsUseTheirDeclaredCategory() {
        assertEquals(AppCategory.STREAMING, AppCategory.of("com.example.video", ApplicationInfo.CATEGORY_VIDEO))
        assertEquals(AppCategory.MUSIC, AppCategory.of("com.example.audio", ApplicationInfo.CATEGORY_AUDIO))
        assertEquals(AppCategory.PHOTOS, AppCategory.of("com.example.photos", ApplicationInfo.CATEGORY_IMAGE))
        assertEquals(AppCategory.GAMES, AppCategory.of("com.example.game", ApplicationInfo.CATEGORY_GAME))
        assertEquals(AppCategory.NEWS, AppCategory.of("com.example.news", ApplicationInfo.CATEGORY_NEWS))
        assertEquals(AppCategory.OTHER, AppCategory.of("com.example.tool", -1))
    }
}
