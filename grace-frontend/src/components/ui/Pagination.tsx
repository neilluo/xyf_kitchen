import { Icon } from './Icon'

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  className?: string
}

export function Pagination({ page, totalPages, onPageChange, className = '' }: PaginationProps) {
  const handlePrevious = () => {
    if (page > 1) {
      onPageChange(page - 1)
    }
  }

  const handleNext = () => {
    if (page < totalPages) {
      onPageChange(page + 1)
    }
  }

  const handlePageClick = (pageNum: number) => {
    if (pageNum !== page) {
      onPageChange(pageNum)
    }
  }

  const getVisiblePages = () => {
    const pages: (number | string)[] = []
    const maxVisible = 5

    if (totalPages <= maxVisible) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i)
      }
    } else {
      if (page <= 3) {
        for (let i = 1; i <= 4; i++) {
          pages.push(i)
        }
        pages.push('...')
        pages.push(totalPages)
      } else if (page >= totalPages - 2) {
        pages.push(1)
        pages.push('...')
        for (let i = totalPages - 3; i <= totalPages; i++) {
          pages.push(i)
        }
      } else {
        pages.push(1)
        pages.push('...')
        for (let i = page - 1; i <= page + 1; i++) {
          pages.push(i)
        }
        pages.push('...')
        pages.push(totalPages)
      }
    }

    return pages
  }

  if (totalPages <= 1) {
    return null
  }

  const visiblePages = getVisiblePages()

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <button
        type="button"
        onClick={handlePrevious}
        disabled={page <= 1}
        className="w-8 h-8 flex items-center justify-center rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-low disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        aria-label="Previous page"
      >
        <Icon name="chevron_left" size={20} />
      </button>

      {visiblePages.map((pageNum, index) => (
        <button
          key={index}
          type="button"
          onClick={() => typeof pageNum === 'number' && handlePageClick(pageNum)}
          disabled={pageNum === '...'}
          className={`w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors ${
            pageNum === page
              ? 'bg-primary text-white'
              : pageNum === '...'
                ? 'text-on-surface-variant cursor-default'
                : 'text-on-surface-variant hover:bg-surface-container-low'
          }`}
          aria-label={typeof pageNum === 'number' ? `Page ${pageNum}` : undefined}
          aria-current={pageNum === page ? 'page' : undefined}
        >
          {pageNum}
        </button>
      ))}

      <button
        type="button"
        onClick={handleNext}
        disabled={page >= totalPages}
        className="w-8 h-8 flex items-center justify-center rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-low disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        aria-label="Next page"
      >
        <Icon name="chevron_right" size={20} />
      </button>
    </div>
  )
}
