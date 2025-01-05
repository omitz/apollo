<template>
    <v-card>
        <v-card-title>
        File: {{path}}
        </v-card-title>
        <v-card-text v-if="this.results_to_display(analytic_results)">
            <div v-if="analytic_results.object_detection && analytic_results.object_detection.length > 0">
                <br>
                <h3>Object Detection:</h3>
                <div
                    v-for="detection in this.parse_object_detection_results(analytic_results.object_detection)"
                    :key="detection.id"
                >
                  <span>Detected {{detection.class}} {{detection.count}} time(s) with a score(s) of </span>
                  <span
                    v-for="score in detection.scores"
                    :key="score.id"
                  >
                    {{Math.round(score.score * 100) / 100}} 
                  </span>
              </div>
            </div>
            <div v-if="analytic_results.video_object_detection && analytic_results.video_object_detection.length > 0">
                <br>
                <h3>Object Detection:</h3>
                <div
                    v-for="detection in analytic_results.video_object_detection"
                    :key="detection.id"
                >
                    <span>Detected {{detection.detection_class}} at </span><span v-for="(time, index) in detection.seconds" :key="index">{{time[0]}} - {{time[1]}}<span v-if="index != detection.seconds.length - 1">, </span></span><span> with a score of {{Math.round(detection.detection_score * 100) / 100}}</span>
                </div>
            </div>
            <div v-if="analytic_results.facial_recognition && analytic_results.facial_recognition.length > 0">
                <br>
                <h3>Facial Recognition:</h3>
                <div
                    v-for="face in analytic_results.facial_recognition"
                    :key="face.id"
                >
                    <div>
                    <span>Face detected: {{face.prediction}} with probability: {{Math.round(face.probability * 100) / 100}} </span>
                    </div>
                </div>
            </div>
            <div v-if="analytic_results.video_facial_recognition && analytic_results.video_facial_recognition.length > 0">
                <br>
                <h3>Facial Recognition:</h3>
                <div
                    v-for="face in analytic_results.video_facial_recognition"
                    :key="face.id"
                >
                    <div>
                    <span>Face detected: {{face.prediction}} with probability: {{Math.round(face.recog_probability * 100) / 100}} </span>
                    </div>
                </div>
            </div>
            <div v-if="analytic_results.scene_classification">
            <br>
            <h3>Scene Classification:</h3>
            <div>
                <div>Scene is {{analytic_results.scene_classification.class_hierarchy}}</div>
                <div><span>Top five classifications: </span><span v-for="_class in analytic_results.scene_classification.top_five_classes" :key="_class">{{_class}}, </span></div>
            </div>
            </div>
            <div v-if="analytic_results.speaker_recognition">
            <br>
            <h3>Speaker Recognition:</h3>
            <div>Speaker: {{analytic_results.speaker_recognition.prediction}} with score: {{Math.round(analytic_results.speaker_recognition.score * 100) / 100}}</div>
            </div>
            <div v-if="analytic_results.ocr && analytic_results.ocr.length > 0">
            <br>
            <h3>Optical Character Recognition:</h3>
            <div
                v-for="ocr_result in analytic_results.ocr"
                :key="ocr_result.id"
            >
                <div>
                <span>{{ocr_result.service_name}} found {{display_text(ocr_result.full_text)}}</span>
                </div>
            </div>
            </div>
            <div v-if="analytic_results.named_entity_recognition_results && analytic_results.named_entity_recognition_results.length > 0 ">
            <br>
            <h3>Named Entity Recognition:</h3>
            <div>
                <div>Mentioned: <span v-for="(mention, index) in analytic_results.named_entity_recognition_results" :key="index">{{mention.entity}}, </span></div>
            </div>
            </div>
            <div v-if="analytic_results.speech_to_text && analytic_results.speech_to_text.length > 0">
            <br>
            <h3>Speech to Text:</h3>
            <div
                v-for="stt_result in analytic_results.speech_to_text"
                :key="stt_result.id"
            >
                <div>
                <span>Audio: {{display_text(stt_result.full_text)}}</span>
                </div>
            </div>
            </div>
            <div v-if="analytic_results.sentiment_results && analytic_results.sentiment_results.length > 0">
                <br>
                <h3>Sentiment Analysis:</h3>
                <div>
                    <div>Sentiment: {{analytic_results.sentiment_results[0].sentiment}}</div>
                    <div>Polarity: {{analytic_results.sentiment_results[0].polarity}}</div>
                </div>
            </div>
        </v-card-text>
        <v-card-text v-else>
            No other results to display
        </v-card-text>
    </v-card>
</template>

<script>

export default {
    props: {
        path: String,
    },

    name: "AnalyticResultsView",
    watch: {
        path: function () {
        //watch the "paths" prop and reset dialog open whenever path is changed
            this.get_results(this.$props.path)    
        },
    },

    data() {
        return {
            analytic_results: [],
        };
    },

    beforeMount() {
        console.log(this.$props.path)
        this.get_results(this.$props.path)
    },

    methods: {
        get_results(path) {
            this.apiGet("search/results/?file=" + path)
                    .then(response => response.json())
                    .then(data => {
                        this.analytic_results = data;
                        console.log(data)
                    });
        },

        display_text(text_result) {
            if (!text_result || !text_result.replace(/^\s+|\s+$/g, '')) {
                return "no text"
            }
            return text_result
        },

        results_to_display(results) {
            if (!results) {
                console.log("no results to display")
                return false;
            }
            if (results.facial_recognition && results.facial_recognition.length > 0) {
                return true;
            }
            if (results.video_facial_recognition && results.video_facial_recognition.length > 0) {
                return true;
            }
            if (results.object_detection && results.object_detection.length > 0) {
                return true;
            }
            if (results.video_object_detection && results.video_object_detection.length > 0) {
                return true;
            }
            if (results.ocr && results.ocr.length > 0) {
                return true;
            }
            if (results.scene_classification) {
                return true;
            }
            if (results.speaker_recognition) {
                return true;
            }
            if (results.named_entity_recognition_results && results.named_entity_recognition_results.length > 0) {
                return true;
            }
            if (results.speech_to_text && results.speech_to_text.length > 0) {
                return true;
            }
            if (results.sentiment_results && results.sentiment_results.length > 0) {
                return true;
            }
            console.log("no results found")
            return false;
        },

        parse_object_detection_results(object_detection_results) {
            var parsed_objects = []

            object_detection_results.forEach((detection) => {

                var object_class = detection['detection_class'];

                var scores = detection['detection_scores'].map((score, i) => {
                    return {"id": i, "score": score}
                });

                var count = detection['detection_scores'].length;

                parsed_objects.push({"class": object_class, "scores": scores, "count": count})
            })

            return parsed_objects;
        }, 
    }
}
</script>