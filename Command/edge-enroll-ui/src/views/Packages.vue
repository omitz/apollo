<template>
    <v-row>
        <v-col cols="9">
        <v-row>
        <v-col cols="3">
        <v-autocomplete
            background-color="primary"
            prepend-inner-icon="$searchicon"
            v-model="search_item"
            :items="packages"
            item-text="group_name"
            item-value="id"
            color="blue-grey"
            label="Search Group"
            filled
            rounded
            dense
          ></v-autocomplete>
        </v-col>
        <v-spacer />
        <v-col cols="3">
            <v-btn class="lower-case" color="accent">
                Create Group
            </v-btn>
        </v-col>
        </v-row>
        <v-row>
        <v-col cols="12">
        <v-data-table
            :headers="headers"
            :items="packages"
            :items-per-page="25"
            item-key="name"
            class="elevation-1"
            show-select
            :footer-props="{
                showFirstLastPage: true,
                firstIcon: '$jumpbackicon',
                lastIcon: '$jumpnexticon',
                prevIcon: '$backicon',
                nextIcon: '$nexticon',
            }"
        >
            <template v-slot:item.status="{ item }">
                <div
                    v-if="item.status == 'Active'"
                >
                    <v-chip
                        color='active'
                    >
                        {{ item.status }}
                    </v-chip>
                </div>
                <div
                    v-else
                >
                    <v-icon class="margin-right">$addicon</v-icon>
                </div>
            </template>
        </v-data-table>
        </v-col>
        </v-row>
        </v-col>
        <v-col cols="3" class="filters-panel">
            Filter
            <v-expansion-panels>
                <v-expansion-panel>
                    <v-expansion-panel-header>
                        Creator
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                        Creator filter
                    </v-expansion-panel-content>
                </v-expansion-panel>
                <v-expansion-panel>
                    <v-expansion-panel-header>
                        Updated
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                        updated filter
                    </v-expansion-panel-content>
                </v-expansion-panel>
                <v-expansion-panel>
                    <v-expansion-panel-header>
                        Status
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                        status filter
                    </v-expansion-panel-content>
                </v-expansion-panel>
            </v-expansion-panels>
        </v-col>
    </v-row>
</template>

<script>
export default {
    name: 'Packages',
    data () {
        return {
            search_item: {},
            headers: [
                { text: 'Group Name', align: 'start', value: 'group_name'},
                { text: 'Profiles', align: 'start', value: 'number_profiles'},
                { text: 'Audio', value: 'number_audio', align: 'start' },
                { text: 'Image', value: 'number_images', align: 'start' },
                { text: 'Creator', value: 'creator', align: 'start' },
                { text: 'Last Synced', value: 'date_last_synced', align: 'start' },
                { text: 'Status', value: 'status', align: 'start' },
            ],
            packages: [
                { id: 0, group_name: "Maghreb Mission Group 21", number_profiles: 10, number_audio: 8, number_images: 24, date_last_synced: "02/05/2021", creator: "Jimmy Swigg", status: "Active" },
                { id: 1, group_name: "Maghreb Mission Group 21", number_profiles: 10, number_audio: 8, number_images: 24, date_last_synced: "02/05/2021", creator: "Jimmy Swigg", status: "Active" },
                { id: 2, group_name: "Maghreb Mission Group 21", number_profiles: 10, number_audio: 8, number_images: 24, date_last_synced: "02/05/2021", creator: "Jimmy Swigg", status: "Active" },
                { id: 3, group_name: "Maghreb Mission Group 21", number_profiles: 10, number_audio: 8, number_images: 24, date_last_synced: "02/05/2021", creator: "Jimmy Swigg", status: "Active" },
            ]
        }
    }
}
</script>

<style scoped>
  .filters-panel {
    background-color: var(--v-tertiary-base);
  }
  .v-input >>> .v-label {
    font-style: italic;
  }
  .v-btn.lower-case {
    text-transform: initial
  }
  .v-chip {
    color: white;
    font-weight: bold;
    padding-right: 20px;
    padding-left: 20px;

  }
</style>