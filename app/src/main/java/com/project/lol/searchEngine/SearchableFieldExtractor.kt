package com.project.lol.searchEngine

fun interface SearchableFieldExtractor<T> {
    fun getSearchableFields(item: T): Array<String>
}