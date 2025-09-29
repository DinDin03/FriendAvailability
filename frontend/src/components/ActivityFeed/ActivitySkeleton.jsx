/**
 * ActivitySkeleton Component - Loading skeleton for activity items
 *
 * This component provides animated loading skeletons that match the layout
 * of actual activity items, creating a smooth loading experience.
 *
 * Features:
 * - Multiple skeleton variants matching activity item layouts
 * - Shimmer animation effects
 * - Responsive design
 * - Configurable count and size
 */
export const ActivitySkeleton = ({
  count = 3,
  size = 'default', // 'compact', 'default', 'detailed'
  showAvatar = true,
  className = ''
}) => {
  // Size-specific styling
  const sizeClasses = {
    compact: {
      container: 'p-3',
      avatar: 'w-8 h-8',
      icon: 'w-6 h-6',
      textLine1: 'h-3',
      textLine2: 'h-3 w-3/4',
      timestamp: 'h-2 w-16'
    },
    default: {
      container: 'p-4',
      avatar: 'w-10 h-10',
      icon: 'w-8 h-8',
      textLine1: 'h-4',
      textLine2: 'h-4 w-2/3',
      timestamp: 'h-3 w-20'
    },
    detailed: {
      container: 'p-5',
      avatar: 'w-12 h-12',
      icon: 'w-10 h-10',
      textLine1: 'h-4',
      textLine2: 'h-4 w-3/4',
      timestamp: 'h-3 w-24'
    }
  };

  const styles = sizeClasses[size];

  /**
   * Single skeleton item component
   */
  const SkeletonItem = ({ index }) => (
    <div className={`border border-gray-200 rounded-lg ${styles.container} ${className}`}>
      <div className="flex items-start space-x-3">
        {/* Avatar Skeleton */}
        {showAvatar && (
          <div className="flex-shrink-0">
            <div className={`${styles.avatar} bg-gray-200 rounded-full animate-pulse`}></div>
          </div>
        )}

        {/* Content Skeleton */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between">
            <div className="flex-1 pr-2">
              {/* Icon and Status Indicator */}
              <div className="flex items-center space-x-2 mb-2">
                <div className={`${styles.icon} bg-gray-200 rounded-full animate-pulse`}></div>
                {/* Randomly show unread indicator on some items */}
                {Math.random() > 0.7 && (
                  <div className="w-2 h-2 bg-blue-200 rounded-full animate-pulse"></div>
                )}
              </div>

              {/* Text Content Skeleton */}
              <div className="space-y-2">
                <div className={`${styles.textLine1} bg-gray-200 rounded animate-pulse`}></div>
                <div className={`${styles.textLine2} bg-gray-200 rounded animate-pulse`}></div>

                {/* Additional line for detailed view */}
                {size === 'detailed' && Math.random() > 0.5 && (
                  <div className="h-3 w-1/2 bg-gray-200 rounded animate-pulse"></div>
                )}
              </div>

              {/* Timestamp and Badge Skeleton */}
              <div className="flex items-center space-x-2 mt-3">
                <div className={`${styles.timestamp} bg-gray-200 rounded animate-pulse`}></div>

                {/* Randomly show priority badge on some items */}
                {Math.random() > 0.8 && (
                  <div className="h-5 w-16 bg-orange-100 rounded-full animate-pulse"></div>
                )}
              </div>
            </div>

            {/* Menu Button Skeleton */}
            <div className="w-6 h-6 bg-gray-200 rounded-full animate-pulse"></div>
          </div>
        </div>
      </div>
    </div>
  );

  /**
   * Group skeleton for grouped view
   */
  const SkeletonGroup = ({ groupIndex, itemCount = 2 }) => (
    <div className="mb-6">
      {/* Group Header Skeleton */}
      <div className="mb-3 px-2">
        <div className="h-4 w-20 bg-gray-200 rounded animate-pulse"></div>
      </div>

      {/* Group Items */}
      <div className="space-y-3">
        {Array.from({ length: itemCount }, (_, index) => (
          <SkeletonItem key={`group-${groupIndex}-item-${index}`} index={index} />
        ))}
      </div>
    </div>
  );

  /**
   * Shimmer overlay component for enhanced loading effect
   */
  const ShimmerOverlay = () => (
    <div className="absolute inset-0 -skew-x-12">
      <div className="w-full h-full bg-gradient-to-r from-transparent via-white to-transparent opacity-30 animate-shimmer"></div>
    </div>
  );

  /**
   * Generate skeleton items
   */
  const generateSkeletons = () => {
    // For grouped display, create groups with varying item counts
    if (Math.random() > 0.5) {
      const groupCount = Math.ceil(count / 3);
      return Array.from({ length: groupCount }, (_, groupIndex) => {
        const itemsInGroup = Math.floor(Math.random() * 3) + 1; // 1-3 items per group
        return (
          <SkeletonGroup
            key={`skeleton-group-${groupIndex}`}
            groupIndex={groupIndex}
            itemCount={itemsInGroup}
          />
        );
      });
    }

    // For flat display, create individual items
    return Array.from({ length: count }, (_, index) => (
      <SkeletonItem key={`skeleton-item-${index}`} index={index} />
    ));
  };

  return (
    <div className="space-y-3 relative">
      {generateSkeletons()}

      {/* Loading indicator */}
      <div className="text-center py-2">
        <div className="inline-flex items-center space-x-2 text-gray-500">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-400"></div>
          <span className="text-sm">Loading activities...</span>
        </div>
      </div>

      {/* Custom CSS for shimmer animation */}
      <style jsx>{`
        @keyframes shimmer {
          0% {
            transform: translateX(-100%);
          }
          100% {
            transform: translateX(100%);
          }
        }

        .animate-shimmer {
          animation: shimmer 2s infinite;
        }
      `}</style>
    </div>
  );
};

/**
 * Compact skeleton variant for inline loading
 */
export const ActivitySkeletonCompact = ({ count = 3 }) => (
  <ActivitySkeleton
    count={count}
    size="compact"
    showAvatar={true}
    className="mb-2"
  />
);

/**
 * Detailed skeleton variant for detailed views
 */
export const ActivitySkeletonDetailed = ({ count = 3 }) => (
  <ActivitySkeleton
    count={count}
    size="detailed"
    showAvatar={true}
    className="mb-4"
  />
);

/**
 * Feed loading skeleton with header
 */
export const ActivityFeedSkeleton = ({
  showHeader = true,
  showFilters = false,
  itemCount = 5
}) => (
  <div className="bg-white rounded-lg shadow-sm">
    {/* Header Skeleton */}
    {showHeader && (
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className="h-6 w-32 bg-gray-200 rounded animate-pulse"></div>
            <div className="h-5 w-16 bg-blue-100 rounded-full animate-pulse"></div>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gray-200 rounded-lg animate-pulse"></div>
            <div className="w-8 h-8 bg-gray-200 rounded-lg animate-pulse"></div>
          </div>
        </div>

        {/* Filters Skeleton */}
        {showFilters && (
          <div className="bg-gray-50 rounded-lg p-4">
            <div className="flex items-center justify-between mb-4">
              <div className="h-4 w-24 bg-gray-200 rounded animate-pulse"></div>
              <div className="h-4 w-16 bg-blue-200 rounded animate-pulse"></div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {Array.from({ length: 4 }, (_, index) => (
                <div key={index} className="space-y-2">
                  <div className="h-3 w-16 bg-gray-200 rounded animate-pulse"></div>
                  <div className="h-8 w-full bg-white border border-gray-200 rounded"></div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    )}

    {/* Content Skeleton */}
    <div className="p-6">
      <ActivitySkeleton count={itemCount} />
    </div>
  </div>
);

/**
 * Individual activity card skeleton for grid layouts
 */
export const ActivityCardSkeleton = ({ showMetadata = true }) => (
  <div className="bg-white border border-gray-200 rounded-lg p-4">
    <div className="flex items-start space-x-3">
      <div className="w-10 h-10 bg-gray-200 rounded-full animate-pulse"></div>
      <div className="flex-1 min-w-0">
        <div className="space-y-2">
          <div className="h-4 w-3/4 bg-gray-200 rounded animate-pulse"></div>
          <div className="h-3 w-1/2 bg-gray-200 rounded animate-pulse"></div>

          {showMetadata && (
            <div className="flex items-center space-x-2 mt-3">
              <div className="h-3 w-16 bg-gray-200 rounded animate-pulse"></div>
              <div className="h-4 w-12 bg-gray-100 rounded-full animate-pulse"></div>
            </div>
          )}
        </div>
      </div>
    </div>
  </div>
);