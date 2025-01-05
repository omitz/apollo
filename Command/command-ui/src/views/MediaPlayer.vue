
<template>
    <MediaManager
        #default="{ paused, playPause, seekTo, currentTime, duration, progress, mediaAttrs, seekSliderAttrs }"
        :type="mediaType"
        :seekingDisabled="seekingDisabled"
        :initialTime="initialTime"
    >
        <div class="MediaPlayer">
            <MediaElement
                ref="video"
                v-bind="mediaAttrs"
                :autoplay="autoplay"
                :volume="volume"
                :muted="muted"
                :loop="loop"
                preload
            >
                <source
                    :src="src"
                >
                <p>Your browser doesn't support HTML5 video</p>
            </MediaElement>
            <div class="box">
                <p class="info-text">
                    The SeekSlider: you can click wherever you want or drag it
                </p>
                <SeekSlider v-bind="seekSliderAttrs">
                    <div class="SliderTrail" />
                    <div
                        slot="handle"
                        class="Handle"
                    />
                </SeekSlider>
            </div>
            <div class="box">
                <div class="Controls">
                    <a v-if="seconds"
                        class="Button"
                        @click="seekTo (getPrevSec())"
                    >
                        {{ 'Prev' }}
                    </a>

                    <a
                        class="Button"
                        @click="playPause"
                    >
                        {{ paused ? 'Play' : 'Pause' }}
                    </a>


                    <a v-if="seconds"
                        class="Button"
                        @click="seekTo (getNextSec())"
                    >
                        {{ 'Next' }}
                    </a>

                    <a v-if = "mediaType =='video'"
                        class="Button"
                        @click="requestFullScreen"
                    >
                        Fullscreen
                    </a>
                </div>
            </div>
            <div class="box">
                <div class="Info">
                    <div>
                        <p class="label">
                            Current time
                        </p>
                        <p class="value">
                            {{ toString(currentTime) }}
                        </p>
                    </div>
                    <div>
                        <p class="label">
                            Duration
                        </p>
                        <p class="value">
                            {{ toString(duration) }}
                        </p>
                    </div>
                    <div>
                        <p class="label">
                            Progress
                        </p>
                        <p class="value">
                            {{ toString(progress) }}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </MediaManager>
</template>

<script>
import VueTypes from 'vue-types'
import {
    MediaManager, MediaElement, SeekSlider,
} from '@adrianhurt/vue-media-manager'

export default {
    name: 'MediaPlayer',
    components: {
        MediaManager,
        MediaElement,
        SeekSlider,
    },


    props: {
        src: VueTypes.string.isRequired,
        mediaType: VueTypes.string.isRequired,
        autoplay: VueTypes.bool.def(false),
        initialTime: VueTypes.number.def(0.0),
        seconds: VueTypes.arrayOf(Number),
    },

    data () {
        return {
            loop: false,
            volume: 1,
            muted: false,
            seekingDisabled: false,
            idx: 0,
            paused: true,
            currentTime: 0,
        }
    },

    created() {
        console.log(this.$props.src)
        console.log(this.$props.mediaType)
    },
    mounted() {
        console.log(this.$props.src)
    },

    methods: {
        toggleLoop () {
            this.loop = !this.loop
        },
        requestFullScreen () {
            this.$refs.video.requestFullScreen()
        },
        toString (num) {
            return typeof num === 'number' ? num.toFixed(2) : '-'
        },
        getPrevSec ()  {
            // console.log ("hello");
            // console.log (`seconds = ${this.seconds}`);
            // alert(event.target);
            if (this.idx <= 0) {
                this.idx = 0;
                // console.log (`hello = ${this.idx}`);
            }
            else {
                this.idx -= 1;
            }
            return (this.seconds[this.idx]);
        },
        getNextSec () {
//            alert(event.target.tagName);
            if (this.idx >= (this.seconds.length -1)) {
                this.idx = this.seconds.length -1;
                // console.log (`hello = ${this.idx}`);
            }
            else {
                this.idx += 1;
            }

            return (this.seconds[this.idx]);
        }
    },
}
</script>

<style scoped lang="scss">
@import '~@adrianhurt/vue-media-manager/dist/vue-media-manager.css';

$width: 100%;
$sliderHeight: 32px;
$sliderTrailHeight: 2px;
$handleSize: 12px;
$primary: #1976D2;
$secondary: white;

.box {
	background-color: rgba($primary, 0.2);
	margin-top: 1px;
	padding: 10px 20px;
	&.d-flex {
		display: flex;
		justify-content: space-between;
		&.center {
			justify-content: center;
		}
	}
}
.inline-box {
	&.expand {
		flex: 1;
	}
	& + & {
		margin-left: 25px;
		padding-left: 25px;
		border-left: 1px solid white;
	}
}
.info-text {
	color: $secondary;
	font-size: 14px;
	margin: 0 0 10px;
}
.MediaPlayer {
	width: $width;
	background-color: rgba($primary, 0.2);
	border-radius: 5px;
	overflow: hidden;
	& > video {
		display: block;
		width: $width;
	}
}
.SeekSlider {
	height: $sliderHeight;
}
.SliderTrail {
	width: 100%;
	height: $sliderTrailHeight;
	border-radius: $sliderTrailHeight;
	background-color: rgba($primary, 0.1);
}
.Handle {
	position: relative;
	top: 50%;
	left: 0;
	width: $handleSize;
	height: $handleSize;
	border-radius: $handleSize;
	margin-top: #{-$handleSize / 2};
	margin-left: #{-$handleSize / 2};
	background-color: $primary;
}
.Controls {
	display: flex;
	justify-content: space-between;
	align-items: center;
}
.Button {
	flex: 1;
	padding: 3px 10px;
	border-radius: 5px;
	display: flex;
    color: $secondary;
	justify-content: center;
	align-items: center;
	background-color: $primary;
	cursor: pointer;
	user-select: none;
	& + & {
		margin-left: 15px;
	}
}
.VolumeSlider {
	width: 200px;
	margin: auto;
	height: $sliderHeight;
}
.VolumeHandle {
	background-color: $secondary;
}
.Info {
	display: flex;
	justify-content: space-between;
	& > * {
		flex: 1;
	}
	p {
		margin: 0 0 3px;
		font-size: 14px;
	}
	.label {
		font-weight: bold;
	}
}
</style>
