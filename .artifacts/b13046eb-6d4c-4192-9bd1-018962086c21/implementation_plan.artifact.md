# Apple Music Animated Album Artwork (Motion Art) Completion Plan

This plan outlines the remaining steps to fully integrate Animated Album Artwork into OneMusic, following the established architecture.

## User Review Required

> [!IMPORTANT]
> - **Track Model Change**: Adding fields to `Track.kt` will impact any code that instantiates `Track` objects (e.g., scanners, dummy data). Default values will be provided to minimize breaking changes.
> - **Battery Impact**: Motion art is visually pleasing but consumes more power. It will only play when the Now Playing sheet is fully expanded.

## Proposed Changes

### Data Layer

#### [MODIFY] [Track.kt](file:///C:/Users/lacdu/.gemini/antigravity/scratch/OneMusic/app/src/main/java/com/example/onemusic/data/model/Track.kt)
- Add `motionArtworkUrl: String? = null` and `hasMotionArtwork: Boolean? = null`.

#### [MODIFY] [MusicRepository.kt](file:///C:/Users/lacdu/.gemini/antigravity/scratch/OneMusic/app/src/main/java/com/example/onemusic/data/repository/MusicRepository.kt)
- Integrate `AppleMusicMotionFetcher` as a dependency.
- Expose `getMotionArtwork(track: Track)` as a Flow.
- Implement `startBackgroundMotionScan(tracks: List<Track>)`.

### Playback Layer

#### [MODIFY] [MusicPlayerController.kt](file:///C:/Users/lacdu/.gemini/antigravity/scratch/OneMusic/app/src/main/java/com/example/onemusic/playback/MusicPlayerController.kt)
- Refactor `fetchMotionArtworkForTrack` to use `MusicRepository`.
- Ensure proper lifecycle handling for the motion artwork fetching job.

### UI Layer

#### [MODIFY] [NowPlayingSheet.kt](file:///C:/Users/lacdu/.gemini/antigravity/scratch/OneMusic/app/src/main/java/com/example/onemusic/ui/screens/player/NowPlayingSheet.kt)
- Refactor the video playback logic into a dedicated internal `MotionArtworkPlayer` component.
- Implement a smooth crossfade between the static artwork and the video once the video is ready to render (avoiding the black/blank flash).
- Optimize lifecycle: Ensure video stops/pauses when the sheet is collapsed or the screen is off.

## Verification Plan

### Automated Tests
- N/A (UI-centric feature)

### Manual Verification
1. Open Now Playing for a known album with Motion Art (e.g., "Midnights" by Taylor Swift or "Harry's House").
2. Verify that the static artwork shows first, followed by a smooth crossfade to the animated video.
3. Verify that the video is silent and loops infinitely.
4. Swipe to the next track and verify the transition.
5. Collapse the sheet and verify that the video resource is managed correctly (paused/released).
