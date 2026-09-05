package com.rubcut.ainews.ui

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.rubcut.ainews.MarkdownRenderer
import com.rubcut.ainews.NewsItem
import com.rubcut.ainews.R
import java.text.DateFormat
import java.util.Date

/** One page per story, swiped horizontally in the article screen. */
class StoryPagerAdapter(
    private var stories: List<NewsItem>
) : RecyclerView.Adapter<StoryPagerAdapter.StoryViewHolder>() {

    /**
     * Free space kept under the text so the last lines can be scrolled clear
     * of the floating bottom bar instead of hiding behind it.
     */
    var bottomInset: Int = 0
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    fun submit(items: List<NewsItem>) {
        stories = items
        notifyDataSetChanged()
    }

    fun itemAt(position: Int): NewsItem? = stories.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(stories[position], bottomInset)
    }

    override fun getItemCount() = stories.size

    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.storyTitle)
        private val meta: TextView = itemView.findViewById(R.id.storyMeta)
        private val body: TextView = itemView.findViewById(R.id.storyBody)

        fun bind(story: NewsItem, bottomInset: Int) {
            itemView.updatePadding(bottom = bottomInset)
            title.text = story.title
            meta.text = listOfNotNull(
                story.source.takeIf { it.isNotBlank() },
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(story.timestamp))
            ).joinToString(" · ")

            if (story.body.isBlank()) {
                body.setText(R.string.no_article_text)
            } else {
                // The model writes GitHub flavoured Markdown; render it as spans.
                body.text = MarkdownRenderer.render(story.body)
                body.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }
}
